package top.rookiestwo.wheatmarket.database.repository;

import java.sql.*;
import java.util.UUID;

public class PurchaseRecordRepository {
    public void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS purchase_record (" +
                "recordID VARCHAR(36) PRIMARY KEY," +
                "marketItemID VARCHAR(36) NOT NULL," +
                "buyerID VARCHAR(36) NOT NULL," +
                "lastPurchaseTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "purchasedAmount INT NOT NULL," +
                "FOREIGN KEY (marketItemID) REFERENCES market_item(MarketItemID) ON DELETE CASCADE," +
                "FOREIGN KEY (buyerID) REFERENCES player_info(uuid)" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void insert(Connection connection, UUID recordID, UUID marketItemID, UUID buyerID, int purchasedAmount) throws SQLException {
        String sql = "INSERT INTO purchase_record (recordID, marketItemID, buyerID, lastPurchaseTime, purchasedAmount) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP(), ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, recordID.toString());
            stmt.setString(2, marketItemID.toString());
            stmt.setString(3, buyerID.toString());
            stmt.setInt(4, purchasedAmount);
            stmt.executeUpdate();
        }
    }

    public Timestamp getLastPurchaseTime(Connection connection, UUID marketItemID, UUID buyerID) throws SQLException {
        String sql = "SELECT MAX(lastPurchaseTime) AS lastTime FROM purchase_record WHERE marketItemID = ? AND buyerID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, marketItemID.toString());
            stmt.setString(2, buyerID.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp("lastTime");
            }
        }
        return null;
    }

    public void deleteByMarketItem(Connection connection, UUID marketItemID) throws SQLException {
        String sql = "DELETE FROM purchase_record WHERE marketItemID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, marketItemID.toString());
            stmt.executeUpdate();
        }
    }

    public void delete(Connection connection, UUID recordID) throws SQLException {
        String sql = "DELETE FROM purchase_record WHERE recordID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, recordID.toString());
            stmt.executeUpdate();
        }
    }

    public int getPurchasedAmountSince(Connection connection, UUID marketItemID, UUID buyerID, Timestamp since) throws SQLException {
        String sql = "SELECT COALESCE(SUM(purchasedAmount), 0) AS purchasedAmount FROM purchase_record WHERE marketItemID = ? AND buyerID = ? AND lastPurchaseTime >= ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, marketItemID.toString());
            stmt.setString(2, buyerID.toString());
            stmt.setTimestamp(3, since);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("purchasedAmount");
            }
        }
        return 0;
    }

    public Timestamp getEarliestPurchaseTimeSince(Connection connection, UUID marketItemID, UUID buyerID, Timestamp since) throws SQLException {
        String sql = "SELECT MIN(lastPurchaseTime) AS earliestTime FROM purchase_record WHERE marketItemID = ? AND buyerID = ? AND lastPurchaseTime >= ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, marketItemID.toString());
            stmt.setString(2, buyerID.toString());
            stmt.setTimestamp(3, since);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp("earliestTime");
            }
        }
        return null;
    }
}
