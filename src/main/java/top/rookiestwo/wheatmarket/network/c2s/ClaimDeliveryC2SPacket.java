package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;
import top.rookiestwo.wheatmarket.service.DeliveryService;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.util.List;
import java.util.UUID;

public class ClaimDeliveryC2SPacket implements CustomPacketPayload {
    public static final Type<ClaimDeliveryC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            WheatMarket.MOD_ID,
            "claim_delivery"
    ));
    public static final StreamCodec<FriendlyByteBuf, ClaimDeliveryC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(
            ClaimDeliveryC2SPacket::encode,
            ClaimDeliveryC2SPacket::new
    );

    private final UUID deliveryId;

    public ClaimDeliveryC2SPacket(UUID deliveryId) {
        this.deliveryId = deliveryId;
    }

    public ClaimDeliveryC2SPacket(FriendlyByteBuf buf) {
        this.deliveryId = buf.readUUID();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(deliveryId);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (WheatMarket.DATABASE == null) return;

            List<ItemStack> inventorySnapshot = player.getInventory().items.stream()
                    .map(ItemStack::copy)
                    .toList();
            WheatMarket.DATABASE.getDeliveryService()
                    .claimDelivery(player.getUUID(), deliveryId, inventorySnapshot, player.server.registryAccess())
                    .thenAccept(result -> player.server.execute(() -> handleResult(player, result)));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle claim delivery packet.", e);
            return null;
        });
    }

    private void handleResult(ServerPlayer player, ServiceResult<DeliveryService.ClaimDeliveryResult> result) {
        if (!result.isSuccess()) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
            return;
        }

        DeliveryService.ClaimDeliveryResult claimResult = result.getValue();
        ItemStack itemStack = ItemStack.parseOptional(player.server.registryAccess(), claimResult.itemNbt());
        itemStack.setCount(claimResult.amount());
        if (!player.getInventory().add(itemStack)) {
            player.drop(itemStack, false);
        }

        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.delivery_claim_success"));
    }
}
