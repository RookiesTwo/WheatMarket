package top.rookiestwo.wheatmarket.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.PacketContext;

import java.util.function.Supplier;

public class OperationResultS2CPacket {
    private boolean success;
    private String messageKey;
    private String[] messageArgs;

    public OperationResultS2CPacket(boolean success, String messageKey, String... messageArgs) {
        this.success = success;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public OperationResultS2CPacket(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
        this.messageKey = buf.readUtf(512);
        int argCount = buf.readVarInt();
        this.messageArgs = new String[argCount];
        for (int i = 0; i < argCount; i++) {
            this.messageArgs[i] = buf.readUtf(256);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeUtf(messageKey, 512);
        buf.writeVarInt(messageArgs.length);
        for (String arg : messageArgs) {
            buf.writeUtf(arg, 256);
        }
    }

    public void apply(Supplier<PacketContext> contextSupplier) {
        PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Component message = Component.translatable(messageKey, (Object[]) messageArgs);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(message, false);
            }
            WheatMarket.LOGGER.debug("Operation result: success={}, key={}", success, messageKey);
        });
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String[] getMessageArgs() {
        return messageArgs;
    }
}
