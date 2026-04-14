package top.rookiestwo.wheatmarket.network.s2c;

import dev.architectury.networking.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import top.rookiestwo.wheatmarket.WheatMarket;

import java.util.function.Supplier;

public class BalanceUpdateS2CPacket {
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

    public void apply(Supplier<PacketContext> contextSupplier) {
        PacketContext context = contextSupplier.get();
        context.queue(() -> {
            WheatMarket.CLIENT_BALANCE = balance;
        });
    }

    public double getBalance() {
        return balance;
    }
}
