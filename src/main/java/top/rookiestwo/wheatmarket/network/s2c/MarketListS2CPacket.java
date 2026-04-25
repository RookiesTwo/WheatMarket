package top.rookiestwo.wheatmarket.network.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MarketListS2CPacket implements CustomPacketPayload {
    public static final Type<MarketListS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "market_list"));
    public static final StreamCodec<FriendlyByteBuf, MarketListS2CPacket> STREAM_CODEC = CustomPacketPayload.codec(MarketListS2CPacket::encode, MarketListS2CPacket::new);

    private List<MarketItemSummary> items;
    private int totalPages;
    private int currentPage;

    public MarketListS2CPacket(List<MarketItem> marketItems, int totalPages, int currentPage) {
        this.items = new ArrayList<>();
        for (MarketItem item : marketItems) {
            this.items.add(new MarketItemSummary(
                    item.getMarketItemID(),
                    item.getItemNBTCompound(),
                    item.getPrice(),
                    item.getAmount(),
                    item.getSellerID(),
                    item.getIfAdmin(),
                    item.getIfSell(),
                    item.getListingTime() != null ? item.getListingTime().getTime() : 0,
                    item.getLastTradeTime() != null ? item.getLastTradeTime().getTime() : -1,
                    item.hasCooldownRestriction()
            ));
        }
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    public MarketListS2CPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(MarketItemSummary.read(buf));
        }
        this.totalPages = buf.readVarInt();
        this.currentPage = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(items.size());
        for (MarketItemSummary item : items) {
            item.write(buf);
        }
        buf.writeVarInt(totalPages);
        buf.writeVarInt(currentPage);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            WheatMarket.CLIENT_MARKET_LIST = this.items;
            WheatMarket.CLIENT_TOTAL_PAGES = this.totalPages;
            WheatMarket.CLIENT_CURRENT_PAGE = this.currentPage;
            WheatMarket.CLIENT_MARKET_LIST_VERSION++;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle market list packet.", e);
            return null;
        });
    }

    public List<MarketItemSummary> getItems() {
        return items;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public static class MarketItemSummary {
        private final UUID marketItemID;
        private final CompoundTag itemNBT;
        private final double price;
        private final int amount;
        private final UUID sellerID;
        private final boolean ifAdmin;
        private final boolean ifSell;
        private final long listingTime;
        private final long lastTradeTime;
        private final boolean hasCooldown;

        public MarketItemSummary(UUID marketItemID, CompoundTag itemNBT, double price, int amount,
                                 UUID sellerID, boolean ifAdmin, boolean ifSell,
                                 long listingTime, long lastTradeTime, boolean hasCooldown) {
            this.marketItemID = marketItemID;
            this.itemNBT = itemNBT;
            this.price = price;
            this.amount = amount;
            this.sellerID = sellerID;
            this.ifAdmin = ifAdmin;
            this.ifSell = ifSell;
            this.listingTime = listingTime;
            this.lastTradeTime = lastTradeTime;
            this.hasCooldown = hasCooldown;
        }

        public static MarketItemSummary read(FriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            CompoundTag nbt = buf.readNbt();
            double price = buf.readDouble();
            int amount = buf.readVarInt();
            UUID sellerID = buf.readUUID();
            boolean ifAdmin = buf.readBoolean();
            boolean ifSell = buf.readBoolean();
            long listingTime = buf.readLong();
            long lastTradeTime = buf.readLong();
            boolean hasCooldown = buf.readBoolean();
            return new MarketItemSummary(id, nbt, price, amount, sellerID, ifAdmin, ifSell, listingTime, lastTradeTime, hasCooldown);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(marketItemID);
            buf.writeNbt(itemNBT);
            buf.writeDouble(price);
            buf.writeVarInt(amount);
            buf.writeUUID(sellerID);
            buf.writeBoolean(ifAdmin);
            buf.writeBoolean(ifSell);
            buf.writeLong(listingTime);
            buf.writeLong(lastTradeTime);
            buf.writeBoolean(hasCooldown);
        }

        public UUID getMarketItemID() { return marketItemID; }
        public CompoundTag getItemNBT() { return itemNBT; }
        public double getPrice() { return price; }
        public int getAmount() { return amount; }
        public UUID getSellerID() { return sellerID; }
        public boolean isIfAdmin() { return ifAdmin; }
        public boolean isIfSell() { return ifSell; }
        public long getListingTime() { return listingTime; }
        public long getLastTradeTime() { return lastTradeTime; }
        public boolean isHasCooldown() { return hasCooldown; }
    }
}
