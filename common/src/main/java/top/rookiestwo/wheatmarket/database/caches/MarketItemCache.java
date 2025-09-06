package top.rookiestwo.wheatmarket.database.caches;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MarketItemCache extends AbstractDatabaseCache {

    private final Map<UUID, MarketItem> cache = new ConcurrentHashMap<>();

    public MarketItemCache(Connection connection) {
        loadAllFromDatabase(connection);
    }

    public Map<UUID, MarketItem> getCache() {
        return cache;
    }

    public void loadAllFromDatabase(Connection connection) {
        String sql = "SELECT * FROM market_item";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID marketItemID = UUID.fromString(rs.getString("MarketItemID"));
                String itemID = rs.getString("itemID");
                CompoundTag itemNBTCompound;
                try {
                    itemNBTCompound = TagParser.parseTag(rs.getString("itemNBTCompound"));
                } catch (Exception e) {
                    itemNBTCompound = null;
                    WheatMarket.LOGGER.error("Failed to parse itemNBTCompound for marketItemID: {}", marketItemID, e);
                }
                UUID sellerID = UUID.fromString(rs.getString("sellerID"));
                Double price = rs.getDouble("price");
                int amount = rs.getInt("amount");
                Timestamp listingTime = rs.getTimestamp("listingTime");
                Boolean ifAdmin = rs.getBoolean("ifAdmin");
                Boolean ifSell = rs.getBoolean("ifSell");
                int cooldownAmount = rs.getInt("cooldownAmount");
                int cooldownTimeInMinutes = rs.getInt("cooldownTimeInMinutes");
                long timeToExpire = rs.getLong("timeToExpire");
                Timestamp lastTradeTime = rs.getTimestamp("lastTradeTime");

                MarketItem item = new MarketItem(
                        marketItemID, itemID, itemNBTCompound, sellerID, price, amount, listingTime,
                        ifAdmin, ifSell, cooldownAmount, cooldownTimeInMinutes,
                        timeToExpire, lastTradeTime
                );

                cache.put(marketItemID, item);
            }
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Failed to load marketItem from database.", e);
        }
    }

    public void saveAllToDatabase(Connection connection) {
        //todo:区分脏数据节约保存时间
        String sql = "INSERT INTO market_item (" +
                "MarketItemID, itemID, itemNBTCompound, sellerID, price, amount, listingTime, " +
                "ifAdmin, ifSell, cooldownAmount, cooldownTimeInMinutes, timeToExpire, lastTradeTime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "itemID=VALUES(itemID), itemNBTCompound=VALUES(itemNBTCompound), sellerID=VALUES(sellerID), price=VALUES(price), " +
                "amount=VALUES(amount), listingTime=VALUES(listingTime), ifAdmin=VALUES(ifAdmin), ifSell=VALUES(ifSell), " +
                "cooldownAmount=VALUES(cooldownAmount), cooldownTimeInMinutes=VALUES(cooldownTimeInMinutes), " +
                "timeToExpire=VALUES(timeToExpire), lastTradeTime=VALUES(lastTradeTime)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (MarketItem item : cache.values()) {
                stmt.setObject(1, item.getMarketItemID());
                stmt.setString(2, item.getItemID());
                stmt.setString(3, item.getItemNBTCompound().toString()); // 序列化 NBT
                stmt.setObject(4, item.getSellerID());
                stmt.setDouble(5, item.getPrice());
                stmt.setInt(6, item.getAmount());
                stmt.setTimestamp(7, item.getListingTime());
                stmt.setBoolean(8, item.getIfAdmin());
                stmt.setBoolean(9, item.getIfSell());
                stmt.setInt(10, item.getCooldownAmount());
                stmt.setInt(11, item.getCooldownTimeInMinutes());
                stmt.setLong(12, item.getTimeToExpire());
                stmt.setTimestamp(13, item.getLastTradeTime());

                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Failed to save market items to database", e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                WheatMarket.LOGGER.error("Failed to rollback transaction", ex);
            }
        }
    }
}