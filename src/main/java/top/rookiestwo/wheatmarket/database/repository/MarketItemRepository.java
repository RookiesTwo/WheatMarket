package top.rookiestwo.wheatmarket.database.repository;

import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

public class MarketItemRepository {
    public void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS market_item (" +
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
            stmt.execute(sql);
        }
    }

    public void insert(Connection connection, MarketItem item) throws SQLException {
        String sql = "INSERT INTO market_item (MarketItemID, itemID, itemNBTCompound, sellerID, price, amount, " +
                "listingTime, ifAdmin, ifSell, cooldownAmount, cooldownTimeInMinutes, timeToExpire, lastTradeTime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindMarketItem(stmt, item);
            stmt.executeUpdate();
        }
    }

    public void update(Connection connection, MarketItem item) throws SQLException {
        String sql = "UPDATE market_item SET itemID=?, itemNBTCompound=?, sellerID=?, price=?, amount=?, " +
                "listingTime=?, ifAdmin=?, ifSell=?, cooldownAmount=?, cooldownTimeInMinutes=?, " +
                "timeToExpire=?, lastTradeTime=? WHERE MarketItemID=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, item.getItemID());
            stmt.setString(2, item.getItemNBTCompound() != null ? item.getItemNBTCompound().toString() : null);
            stmt.setString(3, item.getSellerID().toString());
            stmt.setDouble(4, item.getPrice());
            stmt.setInt(5, item.getAmount());
            stmt.setTimestamp(6, item.getListingTime());
            stmt.setBoolean(7, item.getIfAdmin());
            stmt.setBoolean(8, item.getIfSell());
            stmt.setInt(9, item.getCooldownAmount());
            stmt.setInt(10, item.getCooldownTimeInMinutes());
            stmt.setLong(11, item.getTimeToExpire());
            stmt.setTimestamp(12, item.getLastTradeTime());
            stmt.setString(13, item.getMarketItemID().toString());
            requireUpdated(stmt.executeUpdate(), "No market item row for " + item.getMarketItemID());
        }
    }

    public void delete(Connection connection, UUID marketItemID) throws SQLException {
        String sql = "DELETE FROM market_item WHERE MarketItemID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, marketItemID.toString());
            requireUpdated(stmt.executeUpdate(), "No market item row for " + marketItemID);
        }
    }

    public MarketItem get(Connection connection, UUID marketItemID) throws SQLException {
        String sql = "SELECT * FROM market_item WHERE MarketItemID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, marketItemID.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return resultSetToMarketItem(rs);
            }
        }
        return null;
    }

    private void bindMarketItem(PreparedStatement stmt, MarketItem item) throws SQLException {
        stmt.setString(1, item.getMarketItemID().toString());
        stmt.setString(2, item.getItemID());
        stmt.setString(3, item.getItemNBTCompound() != null ? item.getItemNBTCompound().toString() : null);
        stmt.setString(4, item.getSellerID().toString());
        stmt.setDouble(5, item.getPrice());
        stmt.setInt(6, item.getAmount());
        stmt.setTimestamp(7, item.getListingTime());
        stmt.setBoolean(8, item.getIfAdmin());
        stmt.setBoolean(9, item.getIfSell());
        stmt.setInt(10, item.getCooldownAmount());
        stmt.setInt(11, item.getCooldownTimeInMinutes());
        stmt.setLong(12, item.getTimeToExpire());
        stmt.setTimestamp(13, item.getLastTradeTime());
    }

    private MarketItem resultSetToMarketItem(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("MarketItemID"));
        String nbtStr = rs.getString("itemNBTCompound");
        net.minecraft.nbt.CompoundTag nbt = null;
        if (nbtStr != null) {
            try {
                nbt = net.minecraft.nbt.TagParser.parseTag(nbtStr);
            } catch (Exception e) {
                WheatMarket.LOGGER.error("Failed to parse NBT for market item: {}", id, e);
            }
        }
        return new MarketItem(
                id,
                rs.getString("itemID"),
                nbt,
                UUID.fromString(rs.getString("sellerID")),
                rs.getDouble("price"),
                rs.getInt("amount"),
                rs.getTimestamp("listingTime"),
                rs.getBoolean("ifAdmin"),
                rs.getBoolean("ifSell"),
                rs.getInt("cooldownAmount"),
                rs.getInt("cooldownTimeInMinutes"),
                rs.getLong("timeToExpire"),
                rs.getTimestamp("lastTradeTime")
        );
    }

    private void requireUpdated(int rows, String message) throws SQLException {
        if (rows == 0) {
            throw new SQLException(message);
        }
    }
}
