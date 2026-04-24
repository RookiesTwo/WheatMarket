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
import top.rookiestwo.wheatmarket.network.s2c.BalanceUpdateS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;
import top.rookiestwo.wheatmarket.service.MarketService;

import java.util.UUID;

public class BuyItemC2SPacket implements CustomPacketPayload {
    public static final Type<BuyItemC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "buy_item"));
    public static final StreamCodec<FriendlyByteBuf, BuyItemC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(BuyItemC2SPacket::encode, BuyItemC2SPacket::new);

    private UUID marketItemID;
    private int amount;

    public BuyItemC2SPacket(UUID marketItemID, int amount) {
        this.marketItemID = marketItemID;
        this.amount = amount;
    }

    public BuyItemC2SPacket(FriendlyByteBuf buf) {
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

            WheatMarket.DATABASE.getMarketService().buyItem(player.getUUID(), marketItemID, amount).thenAccept(result ->
                    player.server.execute(() -> {
                        if (!result.isSuccess()) {
                            WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                            return;
                        }

                        MarketService.PurchaseResult purchase = result.getValue();
                        ItemStack itemStack = ItemStack.parseOptional(player.server.registryAccess(), purchase.itemNbt());
                        itemStack.setCount(purchase.amount());
                        if (!player.getInventory().add(itemStack)) {
                            player.drop(itemStack, false);
                        }

                        WheatMarketNetwork.sendToPlayer(player, new BalanceUpdateS2CPacket(purchase.newBalance()));
                        WheatMarketNetwork.sendToPlayer(player,
                                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.buy_success"));
                    })
            );
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle buy item packet.", e);
            return null;
        });
    }
}
