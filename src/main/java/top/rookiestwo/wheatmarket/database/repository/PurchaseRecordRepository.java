package top.rookiestwo.wheatmarket.database.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

public class PurchaseRecordRepository {
    public void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS purchase_record (" +
                "recordID VARCHAR(36) PRIMARY KEY," +
                "marketItemID VARCHAR(36) NOT NULL," +
                "buyerID VARCHAR(36) NOT NULL," +
                "lastPurchaseTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "purchasedAmount INT NOT NULL," +
                "FOREIGN KEY (marketItemID) REFERENCES market_item(MarketItemID)," +
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
}
