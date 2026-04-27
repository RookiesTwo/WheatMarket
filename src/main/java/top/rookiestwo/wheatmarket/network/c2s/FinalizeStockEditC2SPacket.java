package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.nbt.CompoundTag;
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

public class FinalizeStockEditC2SPacket implements CustomPacketPayload {
    public static final Type<FinalizeStockEditC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "finalize_stock_edit"));
    public static final StreamCodec<FriendlyByteBuf, FinalizeStockEditC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(FinalizeStockEditC2SPacket::encode, FinalizeStockEditC2SPacket::new);

    public FinalizeStockEditC2SPacket() {
    }

    public FinalizeStockEditC2SPacket(FriendlyByteBuf buf) {
    }

    public static void finalizeStockEdit(ServerPlayer player, WheatMarketMenu wheatMarketMenu, boolean notify) {
        if (WheatMarket.DATABASE == null) {
            sendFailure(player, notify, "gui.wheatmarket.operation.failed");
            return;
        }

        WheatMarketMenu.StockEditSnapshot snapshot = wheatMarketMenu.beginStockEditFinalization();
        if (snapshot == null) {
            sendFailure(player, notify, "gui.wheatmarket.operation.failed");
            return;
        }

        if (!isValidFinalStack(snapshot)) {
            wheatMarketMenu.completeStockEditFinalization(false);
            sendFailure(player, notify, "gui.wheatmarket.operation.invalid_amount");
            return;
        }

        CompoundTag expectedTemplate = (CompoundTag) snapshot.template().save(player.server.registryAccess());
        boolean isOp = player.hasPermissions(2);
        WheatMarket.DATABASE.getMarketService()
                .finishStockEdit(player.getUUID(), isOp, snapshot.marketItemId(), expectedTemplate,
                        snapshot.finalAmount(), WheatMarketMenu.ITEM_SELECTION_MAX_AMOUNT)
                .thenAccept(result -> player.server.execute(() -> {
                    if (!result.isSuccess()) {
                        wheatMarketMenu.completeStockEditFinalization(false);
                        if (notify) {
                            WheatMarketNetwork.sendToPlayer(player,
                                    new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                        }
                        return;
                    }

                    wheatMarketMenu.completeStockEditFinalization(true);
                    if (notify) {
                        String messageKey = snapshot.finalAmount() == snapshot.originalAmount()
                                ? "gui.wheatmarket.operation.stock_edit_no_change"
                                : "gui.wheatmarket.operation.stock_edit_success";
                        WheatMarketNetwork.sendToPlayer(player,
                                new OperationResultS2CPacket(true, messageKey, String.valueOf(snapshot.finalAmount())));
                    }
                }));
    }

    private static boolean isValidFinalStack(WheatMarketMenu.StockEditSnapshot snapshot) {
        if (snapshot.template().isEmpty() || snapshot.finalAmount() < 0
                || snapshot.finalAmount() > WheatMarketMenu.ITEM_SELECTION_MAX_AMOUNT) {
            return false;
        }
        ItemStack finalStack = snapshot.finalStack();
        return finalStack.isEmpty() || ItemStack.isSameItemSameComponents(snapshot.template(), finalStack);
    }

    private static void sendFailure(ServerPlayer player, boolean notify, String messageKey) {
        if (notify) {
            WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, messageKey));
        }
    }

    public void encode(FriendlyByteBuf buf) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof WheatMarketMenu wheatMarketMenu) {
                finalizeStockEdit(player, wheatMarketMenu, true);
            }
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle finalize stock edit packet.", e);
            return null;
        });
    }
}
