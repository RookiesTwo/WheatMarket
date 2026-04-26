package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;
import top.rookiestwo.wheatmarket.service.MarketService;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.util.ArrayList;
import java.util.List;

public class RequestMarketListC2SPacket implements CustomPacketPayload {
    public static final Type<RequestMarketListC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "request_market_list"));
    public static final StreamCodec<FriendlyByteBuf, RequestMarketListC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(RequestMarketListC2SPacket::encode, RequestMarketListC2SPacket::new);
    private static final int MAX_SEARCH_ITEM_IDS = 2048;

    private int tradeType;
    private int itemType;
    private int sortType;
    private String searchQuery;
    private List<String> localizedSearchItemIds;
    private int page;
    private int pageSize;

    public RequestMarketListC2SPacket(int tradeType, int itemType, int sortType, String searchQuery, int page, int pageSize) {
        this(tradeType, itemType, sortType, searchQuery, List.of(), page, pageSize);
    }

    public RequestMarketListC2SPacket(int tradeType, int itemType, int sortType, String searchQuery,
                                      List<String> localizedSearchItemIds, int page, int pageSize) {
        this.tradeType = tradeType;
        this.itemType = itemType;
        this.sortType = sortType;
        this.searchQuery = searchQuery;
        this.localizedSearchItemIds = sanitizeLocalizedSearchItemIds(localizedSearchItemIds);
        this.page = page;
        this.pageSize = pageSize;
    }

    public RequestMarketListC2SPacket(FriendlyByteBuf buf) {
        this.tradeType = buf.readVarInt();
        this.itemType = buf.readVarInt();
        this.sortType = buf.readVarInt();
        this.searchQuery = buf.readUtf(256);
        int itemIdCount = buf.readVarInt();
        if (itemIdCount < 0 || itemIdCount > MAX_SEARCH_ITEM_IDS) {
            throw new IllegalArgumentException("Invalid localized search item id count: " + itemIdCount);
        }
        this.localizedSearchItemIds = new ArrayList<>(itemIdCount);
        for (int i = 0; i < itemIdCount; i++) {
            this.localizedSearchItemIds.add(buf.readUtf(256));
        }
        this.page = buf.readVarInt();
        this.pageSize = buf.readVarInt();
    }

    private static List<String> sanitizeLocalizedSearchItemIds(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }
        return itemIds.stream()
                .filter(itemId -> itemId != null && !itemId.isBlank())
                .limit(MAX_SEARCH_ITEM_IDS)
                .toList();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(tradeType);
        buf.writeVarInt(itemType);
        buf.writeVarInt(sortType);
        buf.writeUtf(searchQuery, 256);
        buf.writeVarInt(localizedSearchItemIds.size());
        for (String itemId : localizedSearchItemIds) {
            buf.writeUtf(itemId, 256);
        }
        buf.writeVarInt(page);
        buf.writeVarInt(pageSize);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (WheatMarket.DATABASE == null) return;

            ServiceResult<MarketService.MarketListResult> result = WheatMarket.DATABASE.getMarketService()
                    .requestMarketList(player.getUUID(), tradeType, itemType, sortType, searchQuery,
                            localizedSearchItemIds, page, pageSize);
            if (!result.isSuccess()) {
                WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                return;
            }

            MarketService.MarketListResult marketList = result.getValue();
            WheatMarketNetwork.sendToPlayer(player,
                    new MarketListS2CPacket(marketList.items(), marketList.totalPages(), marketList.currentPage()));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle market list request packet.", e);
            return null;
        });
    }
}
