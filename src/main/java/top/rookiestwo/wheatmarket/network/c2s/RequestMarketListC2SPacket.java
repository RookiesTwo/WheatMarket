package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.caches.MarketItemCache;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RequestMarketListC2SPacket implements CustomPacketPayload {
    public static final Type<RequestMarketListC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "request_market_list"));
    public static final StreamCodec<FriendlyByteBuf, RequestMarketListC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(RequestMarketListC2SPacket::encode, RequestMarketListC2SPacket::new);

    private int tradeType;
    private int itemType;
    private int sortType;
    private String searchQuery;
    private int page;

    public RequestMarketListC2SPacket(int tradeType, int itemType, int sortType, String searchQuery, int page) {
        this.tradeType = tradeType;
        this.itemType = itemType;
        this.sortType = sortType;
        this.searchQuery = searchQuery;
        this.page = page;
    }

    public RequestMarketListC2SPacket(FriendlyByteBuf buf) {
        this.tradeType = buf.readVarInt();
        this.itemType = buf.readVarInt();
        this.sortType = buf.readVarInt();
        this.searchQuery = buf.readUtf(256);
        this.page = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(tradeType);
        buf.writeVarInt(itemType);
        buf.writeVarInt(sortType);
        buf.writeUtf(searchQuery, 256);
        buf.writeVarInt(page);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (WheatMarket.DATABASE == null) return;

            MarketItemCache cache = WheatMarket.DATABASE.getMarketItemCache();
            if (cache == null) return;

            Collection<MarketItem> allItems = cache.getCache().values();
            List<MarketItem> filtered = allItems.stream()
                    .filter(item -> !item.isExpired())
                    .filter(item -> {
                        if (tradeType == 1) return item.getIfSell();
                        if (tradeType == 2) return !item.getIfSell();
                        return true;
                    })
                    .filter(item -> {
                        if (itemType == 1) return item.getIfAdmin();
                        if (itemType == 2) return !item.getIfAdmin();
                        return true;
                    })
                    .filter(item -> {
                        if (searchQuery == null || searchQuery.isEmpty()) return true;
                        return item.getItemID().toLowerCase().contains(searchQuery.toLowerCase());
                    })
                    .sorted((a, b) -> {
                        switch (sortType) {
                            case 1:
                                return a.getItemID().compareToIgnoreCase(b.getItemID());
                            case 2:
                                long aTime = a.getLastTradeTime() != null ? a.getLastTradeTime().getTime() : 0;
                                long bTime = b.getLastTradeTime() != null ? b.getLastTradeTime().getTime() : 0;
                                return Long.compare(bTime, aTime);
                            default:
                                return b.getListingTime().compareTo(a.getListingTime());
                        }
                    })
                    .collect(Collectors.toList());

            int itemsPerPage = 10;
            int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / itemsPerPage));
            int safePage = Math.max(0, Math.min(page, totalPages - 1));
            int fromIndex = safePage * itemsPerPage;
            int toIndex = Math.min(fromIndex + itemsPerPage, filtered.size());
            List<MarketItem> pageItems = filtered.subList(fromIndex, toIndex);

            WheatMarketNetwork.sendToPlayer(player,
                    new MarketListS2CPacket(pageItems, totalPages, safePage));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle market list request packet.", e);
            return null;
        });
    }
}
