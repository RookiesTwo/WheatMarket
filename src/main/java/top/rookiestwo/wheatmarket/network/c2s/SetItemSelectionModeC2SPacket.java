package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class SetItemSelectionModeC2SPacket implements CustomPacketPayload {
    public static final Type<SetItemSelectionModeC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "set_item_selection_mode"));
    public static final StreamCodec<FriendlyByteBuf, SetItemSelectionModeC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(SetItemSelectionModeC2SPacket::encode, SetItemSelectionModeC2SPacket::new);

    private final ItemSelectionMode mode;

    public SetItemSelectionModeC2SPacket(ItemSelectionMode mode) {
        this.mode = mode == null ? ItemSelectionMode.DISABLED : mode;
    }

    public SetItemSelectionModeC2SPacket(FriendlyByteBuf buf) {
        this.mode = ItemSelectionMode.fromNetworkId(buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(mode.getNetworkId());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof WheatMarketMenu wheatMarketMenu) {
                wheatMarketMenu.setItemSelectionMode(mode, player);
            }
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle item selection mode packet.", e);
            return null;
        });
    }
}
