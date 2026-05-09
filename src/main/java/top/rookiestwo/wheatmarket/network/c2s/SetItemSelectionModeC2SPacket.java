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
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class SetItemSelectionModeC2SPacket implements CustomPacketPayload {
    public static final Type<SetItemSelectionModeC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "set_item_selection_mode"));
    public static final StreamCodec<FriendlyByteBuf, SetItemSelectionModeC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(SetItemSelectionModeC2SPacket::encode, SetItemSelectionModeC2SPacket::new);

    private final ItemSelectionMode mode;
    private final CompoundTag initialSelectionNbt;
    private final int initialSelectionAmount;
    private final CompoundTag lockedTemplateNbt;
    private final boolean keepSelectionOnDisable;
    private final int maxSelectionAmount;

    public SetItemSelectionModeC2SPacket(ItemSelectionMode mode) {
        this(mode, null, 0, null, false, 0);
    }

    public SetItemSelectionModeC2SPacket(ItemSelectionMode mode, boolean keepSelectionOnDisable) {
        this(mode, null, 0, null, keepSelectionOnDisable, 0);
    }

    public SetItemSelectionModeC2SPacket(ItemSelectionMode mode, CompoundTag initialSelectionNbt, int initialSelectionAmount, CompoundTag lockedTemplateNbt) {
        this(mode, initialSelectionNbt, initialSelectionAmount, lockedTemplateNbt, false, 0);
    }

    public SetItemSelectionModeC2SPacket(ItemSelectionMode mode, CompoundTag initialSelectionNbt, int initialSelectionAmount,
                                         CompoundTag lockedTemplateNbt, boolean keepSelectionOnDisable) {
        this(mode, initialSelectionNbt, initialSelectionAmount, lockedTemplateNbt, keepSelectionOnDisable, 0);
    }

    public SetItemSelectionModeC2SPacket(ItemSelectionMode mode, CompoundTag initialSelectionNbt, int initialSelectionAmount,
                                         CompoundTag lockedTemplateNbt, boolean keepSelectionOnDisable, int maxSelectionAmount) {
        this.mode = mode == null ? ItemSelectionMode.DISABLED : mode;
        this.initialSelectionNbt = initialSelectionNbt;
        this.initialSelectionAmount = Math.max(0, initialSelectionAmount);
        this.lockedTemplateNbt = lockedTemplateNbt;
        this.keepSelectionOnDisable = keepSelectionOnDisable;
        this.maxSelectionAmount = Math.max(0, maxSelectionAmount);
    }

    public SetItemSelectionModeC2SPacket(FriendlyByteBuf buf) {
        this.mode = ItemSelectionMode.fromNetworkId(buf.readVarInt());
        this.initialSelectionNbt = buf.readNbt();
        this.initialSelectionAmount = buf.readVarInt();
        this.lockedTemplateNbt = buf.readNbt();
        this.keepSelectionOnDisable = buf.readBoolean();
        this.maxSelectionAmount = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(mode.getNetworkId());
        buf.writeNbt(initialSelectionNbt);
        buf.writeVarInt(initialSelectionAmount);
        buf.writeNbt(lockedTemplateNbt);
        buf.writeBoolean(keepSelectionOnDisable);
        buf.writeVarInt(maxSelectionAmount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof WheatMarketMenu wheatMarketMenu) {
                if (mode == ItemSelectionMode.DISABLED && keepSelectionOnDisable) {
                    wheatMarketMenu.deactivateItemSelection(player);
                    return;
                }
                wheatMarketMenu.setCustomMaxSelectionAmount(maxSelectionAmount);
                ItemStack initialSelection = mode == ItemSelectionMode.TRANSFER
                        ? ItemStack.EMPTY
                        : stackFromNbt(player, initialSelectionNbt, initialSelectionAmount);
                wheatMarketMenu.configureItemSelection(
                        mode,
                        initialSelection,
                        stackFromNbt(player, lockedTemplateNbt),
                        player
                );
            }
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle item selection mode packet.", e);
            return null;
        });
    }

    private ItemStack stackFromNbt(ServerPlayer player, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(player.server.registryAccess(), tag);
    }

    private ItemStack stackFromNbt(ServerPlayer player, CompoundTag tag, int amount) {
        ItemStack stack = stackFromNbt(player, tag);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        stack.setCount(amount);
        return stack;
    }
}
