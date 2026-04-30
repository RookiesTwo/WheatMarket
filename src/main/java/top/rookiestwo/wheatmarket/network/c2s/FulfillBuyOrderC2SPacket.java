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
import top.rookiestwo.wheatmarket.network.s2c.BalanceUpdateS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;
import top.rookiestwo.wheatmarket.service.MarketService;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.util.UUID;

public class FulfillBuyOrderC2SPacket implements CustomPacketPayload {
    public static final Type<FulfillBuyOrderC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "fulfill_buy_order"));
    public static final StreamCodec<FriendlyByteBuf, FulfillBuyOrderC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(FulfillBuyOrderC2SPacket::encode, FulfillBuyOrderC2SPacket::new);

    private final UUID marketItemID;
    private final int amount;

    public FulfillBuyOrderC2SPacket(UUID marketItemID, int amount) {
        this.marketItemID = marketItemID;
        this.amount = amount;
    }

    public FulfillBuyOrderC2SPacket(FriendlyByteBuf buf) {
        this.marketItemID = buf.readUUID();
        this.amount = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(marketItemID);
        buf.writeVarInt(amount);
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

            ItemStack selectedStack = wheatMarketMenu.getSelectedItem();
            if (selectedStack.isEmpty()) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_selected_item"));
                return;
            }
            if (amount <= 0) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_amount"));
                return;
            }

            ItemStack suppliedSnapshot = selectedStack.copy();
            WheatMarket.DATABASE.getMarketService()
                    .fulfillBuyOrder(player.getUUID(), marketItemID, amount, suppliedSnapshot, player.server.registryAccess())
                    .thenAccept(result -> player.server.execute(() -> handleFulfillmentResult(player, wheatMarketMenu, result)));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle fulfill buy order packet.", e);
            return null;
        });
    }

    private void handleFulfillmentResult(ServerPlayer player, WheatMarketMenu wheatMarketMenu,
                                         ServiceResult<MarketService.BuyOrderFulfillmentResult> result) {
        if (!result.isSuccess()) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
            return;
        }

        MarketService.BuyOrderFulfillmentResult fulfillment = result.getValue();
        ItemStack expectedTemplate = ItemStack.parseOptional(player.server.registryAccess(), fulfillment.deliveryItem().getItemNBTCompound());
        expectedTemplate.setCount(1);
        if (!wheatMarketMenu.consumeSelectedItemsForListing(expectedTemplate, fulfillment.deliveryItem().getAmount())) {
            WheatMarket.DATABASE.getMarketService().revertBuyOrderFulfillment(fulfillment).thenAccept(rollback ->
                    player.server.execute(() -> sendRollbackResult(player, rollback))
            );
            return;
        }

        if (fulfillment.cleanupRequired()) {
            WheatMarket.DATABASE.getMarketService().completeBuyOrderFulfillmentCleanup(fulfillment.updatedItem().getMarketItemID())
                    .thenAccept(cleanup -> player.server.execute(() -> {
                        if (!cleanup.isSuccess()) {
                            WheatMarket.LOGGER.error("Completed buy order {} could not be cleaned up after fulfillment.",
                                    fulfillment.updatedItem().getMarketItemID());
                        }
                        sendSuccess(player, fulfillment.supplierNewBalance());
                    }));
            return;
        }

        sendSuccess(player, fulfillment.supplierNewBalance());
    }

    private void sendRollbackResult(ServerPlayer player, ServiceResult<Void> rollback) {
        if (!rollback.isSuccess()) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.failed"));
            return;
        }
        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(false, "gui.wheatmarket.operation.insufficient_items"));
    }

    private void sendSuccess(ServerPlayer player, double newBalance) {
        WheatMarketNetwork.sendToPlayer(player, new BalanceUpdateS2CPacket(newBalance));
        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.fulfill_buy_order_success"));
    }
}
