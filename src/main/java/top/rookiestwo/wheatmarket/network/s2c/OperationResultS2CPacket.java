package top.rookiestwo.wheatmarket.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.*;

public class OperationResultS2CPacket implements CustomPacketPayload {
    public static final Type<OperationResultS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "operation_result"));
    public static final StreamCodec<FriendlyByteBuf, OperationResultS2CPacket> STREAM_CODEC = CustomPacketPayload.codec(OperationResultS2CPacket::encode, OperationResultS2CPacket::new);

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Component message = Component.translatable(messageKey, (Object[]) messageArgs);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof WheatMarketOrderConfirmationScreen orderConfirmationScreen
                    && orderConfirmationScreen.handleOperationResult(success, message)) {
                WheatMarket.LOGGER.debug("Operation result consumed by order confirmation: success={}, key={}", success, messageKey);
                return;
            }
            if (minecraft.screen instanceof WheatMarketListingScreen listingScreen
                    && listingScreen.handleOperationResult(success, message)) {
                WheatMarket.LOGGER.debug("Operation result consumed by listing screen: success={}, key={}", success, messageKey);
                return;
            }
            if (minecraft.screen instanceof WheatMarketItemSelectionScreen itemSelectionScreen
                    && itemSelectionScreen.handleOperationResult(success, message)) {
                WheatMarket.LOGGER.debug("Operation result consumed by item selection screen: success={}, key={}", success, messageKey);
                return;
            }
            if (minecraft.screen instanceof WheatMarketItemEditScreen itemEditScreen
                    && itemEditScreen.handleOperationResult(success, message)) {
                WheatMarket.LOGGER.debug("Operation result consumed by item edit screen: success={}, key={}", success, messageKey);
                return;
            }
            if (context.player() != null) {
                context.player().displayClientMessage(message, false);
            }
            if (success && minecraft.screen instanceof WheatMarketMainScreen mainScreen) {
                mainScreen.refreshMarketList();
            }
            WheatMarket.LOGGER.debug("Operation result: success={}, key={}", success, messageKey);
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle operation result packet.", e);
            return null;
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
