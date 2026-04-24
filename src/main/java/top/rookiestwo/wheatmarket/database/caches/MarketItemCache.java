package top.rookiestwo.wheatmarket.database.caches;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MarketItemCache {

    private final Map<UUID, MarketItem> cache = new ConcurrentHashMap<>();

    public MarketItemCache(Connection connection) {
        loadAllFromDatabase(connection);
    }

    public Collection<MarketItem> values() {
        return cache.values();
    }

    public MarketItem get(UUID marketItemID) {
        return cache.get(marketItemID);
    }

    public void put(MarketItem item) {
        cache.put(item.getMarketItemID(), item);
    }

    public void remove(UUID marketItemID) {
        cache.remove(marketItemID);
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

}
