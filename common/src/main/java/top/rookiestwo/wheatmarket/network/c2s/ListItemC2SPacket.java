package top.rookiestwo.wheatmarket.network.c2s;

import dev.architectury.networking.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.caches.MarketItemCache;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;
import top.rookiestwo.wheatmarket.database.tables.MarketItemTable;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Supplier;

public class ListItemC2SPacket {
    private int slotIndex;
    private int amount;
    private double price;
    private boolean ifSell;
    private boolean ifAdmin;
    private boolean ifInfinite;
    private int cooldownAmount;
    private int cooldownTimeInMinutes;

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

    public void apply(Supplier<PacketContext> contextSupplier) {
        PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            if (WheatMarket.DATABASE == null) return;

            MarketItemCache cache = WheatMarket.DATABASE.getMarketItemCache();
            if (cache == null) return;

            if (ifAdmin && !player.hasPermissions(2)) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
                return;
            }

            if (ifInfinite && !player.hasPermissions(2)) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.no_permission"));
                return;
            }

            if (price <= 0) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_price"));
                return;
            }

            if (amount <= 0) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_amount"));
                return;
            }

            ItemStack stackInSlot = player.getInventory().getItem(slotIndex);
            if (stackInSlot.isEmpty()) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.slot_empty"));
                return;
            }

            if (stackInSlot.getCount() < amount && !ifAdmin) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.insufficient_items"));
                return;
            }

            ItemStack listingStack = stackInSlot.copy();
            listingStack.setCount(1);

            MarketItem marketItem = new MarketItem();
            marketItem.setMarketItemID(UUID.randomUUID());
            marketItem.setItemID(listingStack.getItem().toString());
            marketItem.setItemNBTCompound((net.minecraft.nbt.CompoundTag) listingStack.save(player.server.registryAccess()));
            marketItem.setSellerID(player.getUUID());
            marketItem.setPrice(price);
            marketItem.setAmount(ifInfinite ? Integer.MAX_VALUE : amount);
            marketItem.setListingTime(new Timestamp(System.currentTimeMillis()));
            marketItem.setIfAdmin(ifAdmin);
            marketItem.setIfSell(ifSell);
            marketItem.setCooldownAmount(cooldownAmount);
            marketItem.setCooldownTimeInMinutes(cooldownTimeInMinutes);
            marketItem.setTimeToExpire(0);
            marketItem.setLastTradeTime(null);

            if (!ifAdmin) {
                stackInSlot.shrink(amount);
                if (stackInSlot.isEmpty()) {
                    player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
                }
            }

            cache.getCache().put(marketItem.getMarketItemID(), marketItem);
            MarketItemTable.insertMarketItem(WheatMarket.DATABASE.getConnection(), marketItem);

            WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                    new OperationResultS2CPacket(true, "gui.wheatmarket.operation.list_success"));
        });
    }
}
