package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

import java.util.UUID;

public class ReleaseItemEditLockC2SPacket implements CustomPacketPayload {
    public static final Type<ReleaseItemEditLockC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "release_item_edit_lock"));
    public static final StreamCodec<FriendlyByteBuf, ReleaseItemEditLockC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ReleaseItemEditLockC2SPacket::encode, ReleaseItemEditLockC2SPacket::new);

    private final UUID marketItemID;

    public ReleaseItemEditLockC2SPacket(UUID marketItemID) {
        this.marketItemID = marketItemID;
    }

    public ReleaseItemEditLockC2SPacket(FriendlyByteBuf buf) {
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

            WheatMarket.DATABASE.getMarketService().releaseItemEditLock(player.getUUID(), marketItemID);
            if (player.containerMenu instanceof WheatMarketMenu wheatMarketMenu) {
                wheatMarketMenu.clearEditingMarketItemId(marketItemID);
            }
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to release item edit lock.", e);
            return null;
        });
    }
}
