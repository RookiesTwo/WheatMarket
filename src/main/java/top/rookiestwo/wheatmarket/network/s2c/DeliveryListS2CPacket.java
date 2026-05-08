package top.rookiestwo.wheatmarket.network.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.DeliveryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeliveryListS2CPacket implements CustomPacketPayload {
    public static final Type<DeliveryListS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            WheatMarket.MOD_ID,
            "delivery_list"
    ));
    public static final StreamCodec<FriendlyByteBuf, DeliveryListS2CPacket> STREAM_CODEC = CustomPacketPayload.codec(
            DeliveryListS2CPacket::encode,
            DeliveryListS2CPacket::new
    );

    private final List<DeliverySummary> deliveries;
    private final int totalPages;
    private final int currentPage;

    public DeliveryListS2CPacket(List<DeliveryItem> deliveries, int totalPages, int currentPage) {
        this.deliveries = new ArrayList<>(deliveries.size());
        for (DeliveryItem delivery : deliveries) {
            this.deliveries.add(new DeliverySummary(
                    delivery.getDeliveryID(),
                    delivery.getMarketItemID(),
                    delivery.getSourcePlayerID(),
                    delivery.getItemNBTCompound(),
                    delivery.getItemID(),
                    delivery.getAmount(),
                    delivery.getRemainingAmount(),
                    delivery.getCreatedTime() == null ? 0L : delivery.getCreatedTime().getTime(),
                    delivery.getClaimedTime() == null ? 0L : delivery.getClaimedTime().getTime(),
                    DeliverySummary.SOURCE_BUY_ORDER
            ));
        }
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    public DeliveryListS2CPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.deliveries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.deliveries.add(DeliverySummary.read(buf));
        }
        this.totalPages = buf.readVarInt();
        this.currentPage = buf.readVarInt();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(deliveries.size());
        for (DeliverySummary delivery : deliveries) {
            delivery.write(buf);
        }
        buf.writeVarInt(totalPages);
        buf.writeVarInt(currentPage);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            WheatMarket.CLIENT_DELIVERY_LIST = deliveries;
            WheatMarket.CLIENT_DELIVERY_TOTAL_PAGES = totalPages;
            WheatMarket.CLIENT_DELIVERY_CURRENT_PAGE = currentPage;
            WheatMarket.CLIENT_DELIVERY_LIST_VERSION++;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle delivery list packet.", e);
            return null;
        });
    }

    public static class DeliverySummary {
        public static final int SOURCE_BUY_ORDER = 0;
        public static final int SOURCE_DELIST = 1;
        public static final int SOURCE_EXPIRED = 2;
        public static final int SOURCE_UNKNOWN = 3;

        private final UUID deliveryId;
        private final UUID marketItemId;
        private final UUID sourcePlayerId;
        private final CompoundTag itemNbt;
        private final String itemId;
        private final int amount;
        private final int remainingAmount;
        private final long createdTime;
        private final long claimedTime;
        private final int sourceType;

        public DeliverySummary(UUID deliveryId, UUID marketItemId, UUID sourcePlayerId, CompoundTag itemNbt,
                               String itemId, int amount, int remainingAmount, long createdTime, long claimedTime,
                               int sourceType) {
            this.deliveryId = deliveryId;
            this.marketItemId = marketItemId;
            this.sourcePlayerId = sourcePlayerId;
            this.itemNbt = itemNbt;
            this.itemId = itemId;
            this.amount = amount;
            this.remainingAmount = remainingAmount;
            this.createdTime = createdTime;
            this.claimedTime = claimedTime;
            this.sourceType = sourceType;
        }

        public static DeliverySummary read(FriendlyByteBuf buf) {
            return new DeliverySummary(
                    buf.readUUID(),
                    buf.readBoolean() ? buf.readUUID() : null,
                    buf.readBoolean() ? buf.readUUID() : null,
                    buf.readNbt(),
                    buf.readUtf(256),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readVarInt()
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(deliveryId);
            buf.writeBoolean(marketItemId != null);
            if (marketItemId != null) {
                buf.writeUUID(marketItemId);
            }
            buf.writeBoolean(sourcePlayerId != null);
            if (sourcePlayerId != null) {
                buf.writeUUID(sourcePlayerId);
            }
            buf.writeNbt(itemNbt);
            buf.writeUtf(itemId == null ? "" : itemId, 256);
            buf.writeVarInt(amount);
            buf.writeVarInt(remainingAmount);
            buf.writeLong(createdTime);
            buf.writeLong(claimedTime);
            buf.writeVarInt(sourceType);
        }

        public UUID getDeliveryId() {
            return deliveryId;
        }

        public UUID getMarketItemId() {
            return marketItemId;
        }

        public UUID getSourcePlayerId() {
            return sourcePlayerId;
        }

        public CompoundTag getItemNbt() {
            return itemNbt;
        }

        public String getItemId() {
            return itemId;
        }

        public int getAmount() {
            return amount;
        }

        public int getRemainingAmount() {
            return remainingAmount;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public long getClaimedTime() {
            return claimedTime;
        }

        public int getSourceType() {
            return sourceType;
        }
    }
}
