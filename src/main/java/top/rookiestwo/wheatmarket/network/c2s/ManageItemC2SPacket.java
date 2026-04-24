package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.caches.MarketItemCache;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;
import top.rookiestwo.wheatmarket.database.tables.MarketItemTable;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;

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

            MarketItemCache cache = WheatMarket.DATABASE.getMarketItemCache();
            if (cache == null) return;

            MarketItem item = cache.getCache().get(marketItemID);
            if (item == null) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.item_not_found"));
                return;
            }

            boolean isOwner = item.getSellerID().equals(player.getUUID());
            boolean isOp = player.hasPermissions(2);
            if (!isOwner && !isOp) {
                WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
                return;
            }

            switch (action) {
                case ACTION_RESTOCK -> handleRestock(player, cache, item);
                case ACTION_WITHDRAW -> handleWithdraw(player, cache, item);
                case ACTION_CHANGE_PRICE -> handleChangePrice(player, cache, item);
                case ACTION_DELIST -> handleDelist(player, cache, item);
                case ACTION_TOGGLE_ADMIN -> handleToggleAdmin(player, cache, item);
                case ACTION_TOGGLE_INFINITE -> handleToggleInfinite(player, cache, item);
                case ACTION_SET_COOLDOWN -> handleSetCooldown(player, cache, item);
                default -> WheatMarketNetwork.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_action"));
            }
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to handle manage item packet.", e);
            return null;
        });
    }

    private void handleRestock(ServerPlayer player, MarketItemCache cache, MarketItem item) {
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
        stackInSlot.shrink(transferAmount);
        if (stackInSlot.isEmpty()) {
            player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
        }

        item.setAmount(item.getAmount() + transferAmount);
        MarketItemTable.updateMarketItem(WheatMarket.DATABASE.getConnection(), item);

        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.restock_success",
                        String.valueOf(transferAmount)));
    }

    private void handleWithdraw(ServerPlayer player, MarketItemCache cache, MarketItem item) {
        if (amount <= 0 || amount > item.getAmount()) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_amount"));
            return;
        }

        ItemStack itemStack = ItemStack.parseOptional(
                player.server.registryAccess(), item.getItemNBTCompound());
        itemStack.setCount(amount);

        if (!player.getInventory().add(itemStack)) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.inventory_full"));
            return;
        }

        item.setAmount(item.getAmount() - amount);
        if (item.getAmount() <= 0) {
            cache.getCache().remove(item.getMarketItemID());
            MarketItemTable.deleteMarketItem(WheatMarket.DATABASE.getConnection(), item.getMarketItemID());
        } else {
            MarketItemTable.updateMarketItem(WheatMarket.DATABASE.getConnection(), item);
        }

        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.withdraw_success"));
    }

    private void handleChangePrice(ServerPlayer player, MarketItemCache cache, MarketItem item) {
        if (price <= 0) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_price"));
            return;
        }

        item.setPrice(price);
        MarketItemTable.updateMarketItem(WheatMarket.DATABASE.getConnection(), item);

        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.price_change_success"));
    }

    private void handleDelist(ServerPlayer player, MarketItemCache cache, MarketItem item) {
        if (item.getAmount() > 0) {
            ItemStack itemStack = ItemStack.parseOptional(
                    player.server.registryAccess(), item.getItemNBTCompound());
            itemStack.setCount(item.getAmount());
            player.getInventory().add(itemStack);
        }

        cache.getCache().remove(item.getMarketItemID());
        MarketItemTable.deleteMarketItem(WheatMarket.DATABASE.getConnection(), item.getMarketItemID());

        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.delist_success"));
    }

    private void handleToggleAdmin(ServerPlayer player, MarketItemCache cache, MarketItem item) {
        if (!player.hasPermissions(2)) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
            return;
        }

        item.setIfAdmin(!item.getIfAdmin());
        MarketItemTable.updateMarketItem(WheatMarket.DATABASE.getConnection(), item);

        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.toggle_admin_success",
                        String.valueOf(item.getIfAdmin())));
    }

    private void handleToggleInfinite(ServerPlayer player, MarketItemCache cache, MarketItem item) {
        if (!player.hasPermissions(2)) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
            return;
        }

        boolean isInfinite = item.getAmount() == Integer.MAX_VALUE;
        if (isInfinite) {
            item.setAmount(1);
        } else {
            item.setAmount(Integer.MAX_VALUE);
        }
        MarketItemTable.updateMarketItem(WheatMarket.DATABASE.getConnection(), item);

        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.toggle_infinite_success",
                        String.valueOf(!isInfinite)));
    }

    private void handleSetCooldown(ServerPlayer player, MarketItemCache cache, MarketItem item) {
        if (!player.hasPermissions(2)) {
            WheatMarketNetwork.sendToPlayer(player,
                    new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
            return;
        }

        item.setCooldownAmount(cooldownAmount);
        item.setCooldownTimeInMinutes(cooldownTimeInMinutes);
        MarketItemTable.updateMarketItem(WheatMarket.DATABASE.getConnection(), item);

        WheatMarketNetwork.sendToPlayer(player,
                new OperationResultS2CPacket(true, "gui.wheatmarket.operation.cooldown_set_success"));
    }
}
