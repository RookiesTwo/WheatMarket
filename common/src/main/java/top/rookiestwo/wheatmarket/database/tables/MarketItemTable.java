package top.rookiestwo.wheatmarket.database.tables;

import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;

import java.sql.*;
import java.util.UUID;

public class MarketItemTable {

    public static void createTable(Connection connection) {
        String createMarketItemTable = "CREATE TABLE IF NOT EXISTS market_item (" +
                "MarketItemID VARCHAR(36) PRIMARY KEY," +
                "itemID VARCHAR(255) NOT NULL," +
                "itemNBTCompound CLOB," +
                "sellerID VARCHAR(36) NOT NULL," +
                "price DOUBLE NOT NULL," +
                "amount INT NOT NULL," +
                "listingTime DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "ifAdmin BOOLEAN DEFAULT FALSE," +
                "ifSell BOOLEAN DEFAULT TRUE," +
                "cooldownAmount INT DEFAULT 0," +
                "cooldownTimeInMinutes INT DEFAULT 0," +
                "timeToExpire BIGINT DEFAULT 0," +
                "lastTradeTime DATETIME," +
                "FOREIGN KEY (sellerID) REFERENCES player_info(uuid)" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createMarketItemTable);
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Create tables failed.", e);
        }
    }

    public static void insertMarketItem(Connection connection, MarketItem item) {
        String sql = "INSERT INTO market_item (MarketItemID, itemID, itemNBTCompound, sellerID, price, amount, " +
                "listingTime, ifAdmin, ifSell, cooldownAmount, cooldownTimeInMinutes, timeToExpire, lastTradeTime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, item.getMarketItemID().toString());
            pstmt.setString(2, item.getItemID());
            pstmt.setString(3, item.getItemNBTCompound() != null ? item.getItemNBTCompound().toString() : null);
            pstmt.setString(4, item.getSellerID().toString());
            pstmt.setDouble(5, item.getPrice());
            pstmt.setInt(6, item.getAmount());
            pstmt.setTimestamp(7, item.getListingTime());
            pstmt.setBoolean(8, item.getIfAdmin());
            pstmt.setBoolean(9, item.getIfSell());
            pstmt.setInt(10, item.getCooldownAmount());
            pstmt.setInt(11, item.getCooldownTimeInMinutes());
            pstmt.setLong(12, item.getTimeToExpire());
            pstmt.setTimestamp(13, item.getLastTradeTime());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Insert market item failed.", e);
        }
    }

    public static void updateMarketItem(Connection connection, MarketItem item) {
        String sql = "UPDATE market_item SET itemID=?, itemNBTCompound=?, sellerID=?, price=?, amount=?, " +
                "listingTime=?, ifAdmin=?, ifSell=?, cooldownAmount=?, cooldownTimeInMinutes=?, " +
                "timeToExpire=?, lastTradeTime=? WHERE MarketItemID=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, item.getItemID());
            pstmt.setString(2, item.getItemNBTCompound() != null ? item.getItemNBTCompound().toString() : null);
            pstmt.setString(3, item.getSellerID().toString());
            pstmt.setDouble(4, item.getPrice());
            pstmt.setInt(5, item.getAmount());
            pstmt.setTimestamp(6, item.getListingTime());
            pstmt.setBoolean(7, item.getIfAdmin());
            pstmt.setBoolean(8, item.getIfSell());
            pstmt.setInt(9, item.getCooldownAmount());
            pstmt.setInt(10, item.getCooldownTimeInMinutes());
            pstmt.setLong(11, item.getTimeToExpire());
            pstmt.setTimestamp(12, item.getLastTradeTime());
            pstmt.setString(13, item.getMarketItemID().toString());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                WheatMarket.LOGGER.warn("Failed to update market item with ID: {}", item.getMarketItemID());
            }
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Update market item failed.", e);
        }
    }

    public static void deleteMarketItem(Connection connection, UUID marketItemID) {
        String sql = "DELETE FROM market_item WHERE MarketItemID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, marketItemID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Delete market item failed.", e);
        }
    }

    public static MarketItem getMarketItem(Connection connection, UUID marketItemID) {
        String sql = "SELECT * FROM market_item WHERE MarketItemID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, marketItemID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return resultSetToMarketItem(rs);
            }
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Get market item failed.", e);
        }
        return null;
    }

    private static MarketItem resultSetToMarketItem(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("MarketItemID"));
        String itemID = rs.getString("itemID");
        String nbtStr = rs.getString("itemNBTCompound");
        net.minecraft.nbt.CompoundTag nbt = null;
        if (nbtStr != null) {
            try {
                nbt = net.minecraft.nbt.TagParser.parseTag(nbtStr);
            } catch (Exception e) {
                WheatMarket.LOGGER.error("Failed to parse NBT for market item: {}", id, e);
            }
        }
        UUID sellerID = UUID.fromString(rs.getString("sellerID"));
        double price = rs.getDouble("price");
        int amount = rs.getInt("amount");
        Timestamp listingTime = rs.getTimestamp("listingTime");
        boolean ifAdmin = rs.getBoolean("ifAdmin");
        boolean ifSell = rs.getBoolean("ifSell");
        int cooldownAmount = rs.getInt("cooldownAmount");
        int cooldownTimeInMinutes = rs.getInt("cooldownTimeInMinutes");
        long timeToExpire = rs.getLong("timeToExpire");
        Timestamp lastTradeTime = rs.getTimestamp("lastTradeTime");

        return new MarketItem(id, itemID, nbt, sellerID, price, amount, listingTime,
                ifAdmin, ifSell, cooldownAmount, cooldownTimeInMinutes, timeToExpire, lastTradeTime);
    }
}
