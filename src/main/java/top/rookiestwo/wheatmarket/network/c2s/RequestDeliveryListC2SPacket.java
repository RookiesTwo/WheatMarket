package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.DeliveryItem;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.DeliveryListS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.util.List;

public class RequestDeliveryListC2SPacket implements CustomPacketPayload {
    public static final Type<RequestDeliveryListC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            WheatMarket.MOD_ID,
            "request_delivery_list"
    ));
    public static final StreamCodec<FriendlyByteBuf, RequestDeliveryListC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(
            RequestDeliveryListC2SPacket::encode,
            RequestDeliveryListC2SPacket::new
    );

    private final int page;
    private final int pageSize;

    public RequestDeliveryListC2SPacket(int page, int pageSize) {
        this.page = Math.max(0, page);
        this.pageSize = Math.max(1, pageSize);
    }

    public RequestDeliveryListC2SPacket(FriendlyByteBuf buf) {
        this.page = buf.readVarInt();
        this.pageSize = buf.readVarInt();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(page);
        buf.writeVarInt(pageSize);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (WheatMarket.DATABASE == null) return;

            WheatMarket.DATABASE.getDeliveryService().listDeliveries(player.getUUID()).thenAccept(result ->
                    player.server.execute(() -> handleResult(player, result))
            );
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle delivery list request packet.", e);
            return null;
        });
    }

    private void handleResult(ServerPlayer player, ServiceResult<List<DeliveryItem>> result) {
        if (!result.isSuccess()) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
            return;
        }

        List<DeliveryItem> deliveries = result.getValue();
        int safePageSize = Math.max(1, pageSize);
        int totalPages = deliveries.isEmpty() ? 1 : (deliveries.size() + safePageSize - 1) / safePageSize;
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = Math.min(safePage * safePageSize, deliveries.size());
        int toIndex = Math.min(fromIndex + safePageSize, deliveries.size());

        WheatMarketNetwork.sendToPlayer(player,
                new DeliveryListS2CPacket(deliveries.subList(fromIndex, toIndex), totalPages, safePage));
    }
}
