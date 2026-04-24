package top.rookiestwo.wheatmarket.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.caches.MarketItemCache;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;
import top.rookiestwo.wheatmarket.database.tables.MarketItemTable;
import top.rookiestwo.wheatmarket.database.tables.PlayerInfoTable;
import top.rookiestwo.wheatmarket.database.tables.PurchaseRecordTable;
import top.rookiestwo.wheatmarket.network.PacketContext;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.BalanceUpdateS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.OperationResultS2CPacket;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Supplier;

public class BuyItemC2SPacket {
    private UUID marketItemID;
    private int amount;

    public BuyItemC2SPacket(UUID marketItemID, int amount) {
        this.marketItemID = marketItemID;
        this.amount = amount;
    }

    public BuyItemC2SPacket(FriendlyByteBuf buf) {
        this.marketItemID = buf.readUUID();
        this.amount = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(marketItemID);
        buf.writeVarInt(amount);
    }

    public void apply(Supplier<PacketContext> contextSupplier) {
        PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            if (WheatMarket.DATABASE == null) return;

            MarketItemCache cache = WheatMarket.DATABASE.getMarketItemCache();
            if (cache == null) return;

            MarketItem item = cache.getCache().get(marketItemID);
            if (item == null) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.item_not_found"));
                return;
            }

            if (item.isExpired()) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.item_expired"));
                return;
            }

            if (!item.getIfSell()) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.not_for_sale"));
                return;
            }

            if (amount <= 0 || amount > item.getAmount()) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.invalid_amount"));
                return;
            }

            double totalCost = item.getPrice() * amount;
            double buyerBalance = PlayerInfoTable.getPlayerBalance(WheatMarket.DATABASE.getConnection(), player.getUUID());
            if (buyerBalance < totalCost) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.insufficient_balance"));
                return;
            }

            if (item.hasCooldownRestriction()) {
                Timestamp lastPurchase = PurchaseRecordTable.getLastPurchaseTime(
                        WheatMarket.DATABASE.getConnection(), marketItemID, player.getUUID());
                if (lastPurchase != null) {
                    long cooldownMs = (long) item.getCooldownTimeInMinutes() * 60 * 1000;
                    long elapsed = System.currentTimeMillis() - lastPurchase.getTime();
                    if (elapsed < cooldownMs) {
                        long remainingMin = (cooldownMs - elapsed) / 60000 + 1;
                        WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                                new OperationResultS2CPacket(false, "gui.wheatmarket.operation.cooldown_active",
                                        String.valueOf(remainingMin)));
                        return;
                    }
                }
            }

            ItemStack itemStack = ItemStack.parseOptional(
                    player.server.registryAccess(), item.getItemNBTCompound());
            itemStack.setCount(amount);

            if (!player.getInventory().add(itemStack)) {
                WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                        new OperationResultS2CPacket(false, "gui.wheatmarket.operation.inventory_full"));
                return;
            }

            PlayerInfoTable.addPlayerBalance(WheatMarket.DATABASE.getConnection(), player.getUUID(), -totalCost);

            if (!item.getIfAdmin()) {
                PlayerInfoTable.addPlayerBalance(WheatMarket.DATABASE.getConnection(), item.getSellerID(), totalCost);
            }

            item.reduceAmount(amount);
            MarketItemTable.updateMarketItem(WheatMarket.DATABASE.getConnection(), item);

            PurchaseRecordTable.insertPurchaseRecord(WheatMarket.DATABASE.getConnection(),
                    UUID.randomUUID(), marketItemID, player.getUUID(), amount);

            double newBalance = PlayerInfoTable.getPlayerBalance(WheatMarket.DATABASE.getConnection(), player.getUUID());
            WheatMarketNetwork.CHANNEL.sendToPlayer(player, new BalanceUpdateS2CPacket(newBalance));
            WheatMarketNetwork.CHANNEL.sendToPlayer(player,
                    new OperationResultS2CPacket(true, "gui.wheatmarket.operation.buy_success"));
        });
    }
}
