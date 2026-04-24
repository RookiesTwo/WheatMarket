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
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;
import top.rookiestwo.wheatmarket.service.MarketService;

import java.util.UUID;

public class ManageItemC2SPacket implements CustomPacketPayload {
    public static final Type<ManageItemC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "manage_item"));
    public static final StreamCodec<FriendlyByteBuf, ManageItemC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ManageItemC2SPacket::encode, ManageItemC2SPacket::new);

    public static final int ACTION_RESTOCK = 0;
    public static final int ACTION_WITHDRAW = 1;
    public static final int ACTION_CHANGE_PRICE = 2;
    public static final int ACTION_DELIST = 3;
    public static final int ACTION_TOGGLE_ADMIN = 4;
    public static final int ACTION_TOGGLE_INFINITE = 5;
    public static final int ACTION_SET_COOLDOWN = 6;

    private UUID marketItemID;
    private int action;
    private int amount;
    private double price;
    private int slotIndex;
    private int cooldownAmount;
    private int cooldownTimeInMinutes;

    public ManageItemC2SPacket(UUID marketItemID, int action, int amount, double price,
                               int slotIndex, int cooldownAmount, int cooldownTimeInMinutes) {
        this.marketItemID = marketItemID;
        this.action = action;
        this.amount = amount;
        this.price = price;
        this.slotIndex = slotIndex;
        this.cooldownAmount = cooldownAmount;
        this.cooldownTimeInMinutes = cooldownTimeInMinutes;
    }

    public ManageItemC2SPacket(FriendlyByteBuf buf) {
        this.marketItemID = buf.readUUID();
        this.action = buf.readVarInt();
        this.amount = buf.readVarInt();
        this.price = buf.readDouble();
        this.slotIndex = buf.readVarInt();
        this.cooldownAmount = buf.readVarInt();
        this.cooldownTimeInMinutes = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(marketItemID);
        buf.writeVarInt(action);
        buf.writeVarInt(amount);
        buf.writeDouble(price);
        buf.writeVarInt(slotIndex);
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

            boolean isOp = player.hasPermissions(2);
            switch (action) {
                case ACTION_RESTOCK -> handleRestock(player, isOp);
                case ACTION_WITHDRAW -> handleWithdraw(player, isOp);
                case ACTION_CHANGE_PRICE -> handleChangePrice(player, isOp);
                case ACTION_DELIST -> handleDelist(player, isOp);
                case ACTION_TOGGLE_ADMIN -> handleToggleAdmin(player, isOp);
                case ACTION_TOGGLE_INFINITE -> handleToggleInfinite(player, isOp);
                case ACTION_SET_COOLDOWN -> handleSetCooldown(player, isOp);
                default -> WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_action"));
            }
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle manage item packet.", e);
            return null;
        });
    }

    private void handleRestock(ServerPlayer player, boolean isOp) {
        if (slotIndex < 0 || slotIndex >= player.getInventory().getContainerSize()) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_slot"));
            return;
        }

        ItemStack stackInSlot = player.getInventory().getItem(slotIndex);
        if (stackInSlot.isEmpty()) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.slot_empty"));
            return;
        }

        int transferAmount = Math.min(amount, stackInSlot.getCount());
        if (transferAmount <= 0) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_amount"));
            return;
        }

        ItemStack originalStack = stackInSlot.copy();

        WheatMarket.DATABASE.getMarketService().restock(player.getUUID(), isOp, marketItemID, transferAmount).thenAccept(result ->
                player.server.execute(() -> {
                    if (!result.isSuccess()) {
                        WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                        return;
                    }

                    ItemStack currentStack = player.getInventory().getItem(slotIndex);
                    if (!ItemStack.isSameItemSameComponents(currentStack, originalStack) || currentStack.getCount() < transferAmount) {
                        WheatMarket.DATABASE.getMarketService().withdraw(player.getUUID(), isOp, marketItemID, transferAmount);
                        WheatMarketNetwork.sendToPlayer(player,
                                new OperationResultS2CPacket(false, "gui.wheatmarket.operation.insufficient_items"));
                        return;
                    }

                    currentStack.shrink(transferAmount);
                    if (currentStack.isEmpty()) {
                        player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
                    }

                    WheatMarketNetwork.sendToPlayer(player,
                            new OperationResultS2CPacket(true, "gui.wheatmarket.operation.restock_success",
                                    String.valueOf(transferAmount)));
                })
        );
    }

    private void handleWithdraw(ServerPlayer player, boolean isOp) {
        if (amount <= 0) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_amount"));
            return;
        }

        WheatMarket.DATABASE.getMarketService().withdraw(player.getUUID(), isOp, marketItemID, amount).thenAccept(result ->
                player.server.execute(() -> {
                    if (!result.isSuccess()) {
                        WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                        return;
                    }
                    giveItemToPlayer(player, result.getValue().itemNbt(), result.getValue().amount());
                    WheatMarketNetwork.sendToPlayer(player,
                            new OperationResultS2CPacket(true, "gui.wheatmarket.operation.withdraw_success"));
                })
        );
    }

    private void handleChangePrice(ServerPlayer player, boolean isOp) {
        if (price <= 0) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_price"));
            return;
        }

        WheatMarket.DATABASE.getMarketService().changePrice(player.getUUID(), isOp, marketItemID, price).thenAccept(result ->
                player.server.execute(() -> sendSimpleResult(player, result, "gui.wheatmarket.operation.price_change_success"))
        );
    }

    private void handleDelist(ServerPlayer player, boolean isOp) {
        WheatMarket.DATABASE.getMarketService().delist(player.getUUID(), isOp, marketItemID).thenAccept(result ->
                player.server.execute(() -> {
                    if (!result.isSuccess()) {
                        WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                        return;
                    }
                    if (result.getValue().amount() > 0) {
                        giveItemToPlayer(player, result.getValue().itemNbt(), result.getValue().amount());
                    }
                    WheatMarketNetwork.sendToPlayer(player,
                            new OperationResultS2CPacket(true, "gui.wheatmarket.operation.delist_success"));
                })
        );
    }

    private void handleToggleAdmin(ServerPlayer player, boolean isOp) {
        if (!player.hasPermissions(2)) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
            return;
        }

        WheatMarket.DATABASE.getMarketService().toggleAdmin(player.getUUID(), isOp, marketItemID).thenAccept(result ->
                player.server.execute(() -> {
                    if (!result.isSuccess()) {
                        WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                        return;
                    }
                    WheatMarketNetwork.sendToPlayer(player,
                            new OperationResultS2CPacket(true, "gui.wheatmarket.operation.toggle_admin_success",
                                    String.valueOf(result.getValue().updatedItem().getIfAdmin())));
                })
        );
    }

    private void handleToggleInfinite(ServerPlayer player, boolean isOp) {
        if (!player.hasPermissions(2)) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
            return;
        }

        WheatMarket.DATABASE.getMarketService().toggleInfinite(player.getUUID(), isOp, marketItemID).thenAccept(result ->
                player.server.execute(() -> {
                    if (!result.isSuccess()) {
                        WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
                        return;
                    }
                    boolean isInfinite = result.getValue().updatedItem().getAmount() == Integer.MAX_VALUE;
                    WheatMarketNetwork.sendToPlayer(player,
                            new OperationResultS2CPacket(true, "gui.wheatmarket.operation.toggle_infinite_success",
                                    String.valueOf(isInfinite)));
                })
        );
    }

    private void handleSetCooldown(ServerPlayer player, boolean isOp) {
        if (!player.hasPermissions(2)) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
            return;
        }

        WheatMarket.DATABASE.getMarketService().setCooldown(player.getUUID(), isOp, marketItemID, cooldownAmount, cooldownTimeInMinutes).thenAccept(result ->
                player.server.execute(() -> sendSimpleResult(player, result, "gui.wheatmarket.operation.cooldown_set_success"))
        );
    }

    private void giveItemToPlayer(ServerPlayer player, net.minecraft.nbt.CompoundTag itemNbt, int amount) {
        ItemStack itemStack = ItemStack.parseOptional(player.server.registryAccess(), itemNbt);
        itemStack.setCount(amount);
        if (!player.getInventory().add(itemStack)) {
            player.drop(itemStack, false);
        }
    }

    private void sendSimpleResult(ServerPlayer player, top.rookiestwo.wheatmarket.service.result.ServiceResult<?> result, String successKey) {
        if (!result.isSuccess()) {
            WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(false, result.getMessageKey(), result.getMessageArgs()));
            return;
        }
        WheatMarketNetwork.sendToPlayer(player, new OperationResultS2CPacket(true, successKey));
    }
}
