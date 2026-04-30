package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.util.UUID;

public class AcquireItemEditLockC2SPacket implements CustomPacketPayload {
    public static final Type<AcquireItemEditLockC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "acquire_item_edit_lock"));
    public static final StreamCodec<FriendlyByteBuf, AcquireItemEditLockC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(AcquireItemEditLockC2SPacket::encode, AcquireItemEditLockC2SPacket::new);

    private final UUID marketItemID;

    public AcquireItemEditLockC2SPacket(UUID marketItemID) {
        this.marketItemID = marketItemID;
    }

    public AcquireItemEditLockC2SPacket(FriendlyByteBuf buf) {
        this.marketItemID = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(marketItemID);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (WheatMarket.DATABASE == null) return;
            if (!(player.containerMenu instanceof WheatMarketMenu wheatMarketMenu)) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.failed"));
                return;
            }

            boolean isOp = player.hasPermissions(2);
            ServiceResult<Void> result = WheatMarket.DATABASE.getMarketService()
                    .acquireItemEditLock(player.getUUID(), isOp, marketItemID);
            if (result.isSuccess()) {
                wheatMarketMenu.setEditingMarketItemId(marketItemID);
            }
            WheatMarketNetwork.sendToPlayer(player, result.isSuccess()
                    ? new OperationResultS2CPacket(true, "gui.wheatmarket.operation.edit_lock_acquired")
                    : new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to acquire item edit lock.", e);
            return null;
        });
    }
}
