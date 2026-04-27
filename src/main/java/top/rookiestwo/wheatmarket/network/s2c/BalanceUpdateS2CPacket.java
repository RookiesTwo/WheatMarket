package top.rookiestwo.wheatmarket.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.WheatMarketMainScreen;

public class BalanceUpdateS2CPacket implements CustomPacketPayload {
    public static final Type<BalanceUpdateS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "balance_update"));
    public static final StreamCodec<FriendlyByteBuf, BalanceUpdateS2CPacket> STREAM_CODEC = CustomPacketPayload.codec(BalanceUpdateS2CPacket::encode, BalanceUpdateS2CPacket::new);

    private double balance;

    public BalanceUpdateS2CPacket(double balance) {
        this.balance = balance;
    }

    public BalanceUpdateS2CPacket(FriendlyByteBuf buf) {
        this.balance = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(balance);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            WheatMarket.CLIENT_BALANCE = balance;
            if (Minecraft.getInstance().screen instanceof WheatMarketMainScreen mainScreen) {
                mainScreen.handleBalanceUpdate(balance);
            }
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle balance update packet.", e);
            return null;
        });
    }

    public double getBalance() {
        return balance;
    }
}
