package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;

import java.util.UUID;

public class BeginStockEditC2SPacket implements CustomPacketPayload {
    public static final Type<BeginStockEditC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "begin_stock_edit"));
    public static final StreamCodec<FriendlyByteBuf, BeginStockEditC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(BeginStockEditC2SPacket::encode, BeginStockEditC2SPacket::new);

    private final UUID marketItemID;

    public BeginStockEditC2SPacket(UUID marketItemID) {
        this.marketItemID = marketItemID;
    }

    public BeginStockEditC2SPacket(FriendlyByteBuf buf) {
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
            WheatMarket.DATABASE.getMarketService()
                    .beginStockEdit(player.getUUID(), isOp, marketItemID, WheatMarketMenu.ITEM_SELECTION_MAX_AMOUNT)
                    .thenAccept(result -> player.server.execute(() -> {
                        if (!result.isSuccess()) {
                            WheatMarketNetwork.sendToPlayer(player,
                                    new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                            return;
                        }

                        if (player.containerMenu != wheatMarketMenu) {
                            WheatMarket.DATABASE.getMarketService().finishStockEdit(
                                    player.getUUID(),
                                    isOp,
                                    marketItemID,
                                    result.getValue().itemNbt(),
                                    result.getValue().originalAmount(),
                                    WheatMarketMenu.ITEM_SELECTION_MAX_AMOUNT
                            );
                            return;
                        }

                        ItemStack template = ItemStack.parseOptional(player.server.registryAccess(), result.getValue().itemNbt());
                        template.setCount(1);
                        wheatMarketMenu.configureStockEdit(marketItemID, template, result.getValue().originalAmount(), player);
                    }));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle begin stock edit packet.", e);
            return null;
        });
    }
}
