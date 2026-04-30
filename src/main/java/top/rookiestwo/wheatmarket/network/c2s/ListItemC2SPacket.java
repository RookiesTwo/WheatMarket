package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;

import java.sql.Timestamp;
import java.util.UUID;

public class ListItemC2SPacket implements CustomPacketPayload {
    public static final Type<ListItemC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "list_item"));
    public static final StreamCodec<FriendlyByteBuf, ListItemC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ListItemC2SPacket::encode, ListItemC2SPacket::new);
    private static final int MAX_BUY_ORDER_AMOUNT = 999;

    private int slotIndex;
    private int amount;
    private double price;
    private boolean ifSell;
    private boolean ifAdmin;
    private boolean ifInfinite;
    private int cooldownAmount;
    private int cooldownTimeInMinutes;

    public ListItemC2SPacket(int amount, double price, boolean ifSell,
                             boolean ifAdmin, boolean ifInfinite,
                             int cooldownAmount, int cooldownTimeInMinutes) {
        this(WheatMarketMenu.ITEM_SELECTION_SLOT_INDEX, amount, price, ifSell, ifAdmin, ifInfinite,
                cooldownAmount, cooldownTimeInMinutes);
    }

    public ListItemC2SPacket(int slotIndex, int amount, double price, boolean ifSell,
                             boolean ifAdmin, boolean ifInfinite,
                             int cooldownAmount, int cooldownTimeInMinutes) {
        this.slotIndex = slotIndex;
        this.amount = amount;
        this.price = price;
        this.ifSell = ifSell;
        this.ifAdmin = ifAdmin;
        this.ifInfinite = ifInfinite;
        this.cooldownAmount = cooldownAmount;
        this.cooldownTimeInMinutes = cooldownTimeInMinutes;
    }

    public ListItemC2SPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readVarInt();
        this.amount = buf.readVarInt();
        this.price = buf.readDouble();
        this.ifSell = buf.readBoolean();
        this.ifAdmin = buf.readBoolean();
        this.ifInfinite = buf.readBoolean();
        this.cooldownAmount = buf.readVarInt();
        this.cooldownTimeInMinutes = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeVarInt(amount);
        buf.writeDouble(price);
        buf.writeBoolean(ifSell);
        buf.writeBoolean(ifAdmin);
        buf.writeBoolean(ifInfinite);
        buf.writeVarInt(cooldownAmount);
        buf.writeVarInt(cooldownTimeInMinutes);
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

            if (ifAdmin && !player.hasPermissions(2)) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
                return;
            }

            if (ifInfinite && !player.hasPermissions(2)) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
                return;
            }

            if (price <= 0) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_price"));
                return;
            }

            if (amount <= 0) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_amount"));
                return;
            }

            if (!ifSell && amount > MAX_BUY_ORDER_AMOUNT) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_amount"));
                return;
            }

            ItemStack selectedStack = wheatMarketMenu.getSelectedItem();
            if (selectedStack.isEmpty()) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_selected_item"));
                return;
            }

            if (ifSell && !ifAdmin && selectedStack.getCount() < amount) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.insufficient_items"));
                return;
            }

            ItemStack listingStack = selectedStack.copy();
            listingStack.setCount(1);

            MarketItem marketItem = new MarketItem();
            marketItem.setMarketItemID(UUID.randomUUID());
            marketItem.setItemID(listingStack.getItem().toString());
            marketItem.setItemNBTCompound((net.minecraft.nbt.CompoundTag) listingStack.save(player.server.registryAccess()));
            marketItem.setSellerID(player.getUUID());
            marketItem.setPrice(price);
            marketItem.setAmount(amount);
            marketItem.setIfInfinite(ifInfinite);
            marketItem.setListingTime(new Timestamp(System.currentTimeMillis()));
            marketItem.setIfAdmin(ifAdmin);
            marketItem.setIfSell(ifSell);
            marketItem.setCooldownAmount(cooldownAmount);
            marketItem.setCooldownTimeInMinutes(cooldownTimeInMinutes);
            marketItem.setTimeToExpire(0);
            marketItem.setLastTradeTime(null);

            WheatMarket.DATABASE.getMarketService().listItem(marketItem).thenAccept(result ->
                    player.server.execute(() -> {
                        if (!result.isSuccess()) {
                            WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                            return;
                        }

                        if (ifSell && !ifAdmin) {
                            if (!wheatMarketMenu.consumeSelectedItemsForListing(listingStack, amount)) {
                                WheatMarket.DATABASE.getMarketService().delist(player.getUUID(), false, marketItem.getMarketItemID());
                                WheatMarketNetwork.sendToPlayer(player,
                                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.insufficient_items"));
                                return;
                            }
                        } else if (!ifSell) {
                            wheatMarketMenu.clearSampleSelection(listingStack);
                        }

                        WheatMarketNetwork.sendToPlayer(player,
                                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.list_success"));
                    })
            );
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle list item packet.", e);
            return null;
        });
    }
}
