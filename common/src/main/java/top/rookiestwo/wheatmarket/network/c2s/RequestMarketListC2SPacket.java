package top.rookiestwo.wheatmarket.network.c2s;

import dev.architectury.networking.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.caches.MarketItemCache;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;
import top.rookiestwo.wheatmarket.database.tables.MarketItemTable;
import top.rookiestwo.wheatmarket.database.tables.PlayerInfoTable;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.BalanceUpdateS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;

import java.sql.Timestamp;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RequestMarketListC2SPacket {
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

    public void apply(Supplier<PacketContext> contextSupplier) {
        PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
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

            WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                    new MarketListS2CPacket(pageItems, totalPages, safePage));
        });
    }
}
