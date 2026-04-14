package top.rookiestwo.wheatmarket.database.tables;

import top.rookiestwo.wheatmarket.WheatMarket;

import java.sql.*;
import java.util.UUID;

public class PurchaseRecordTable {

    public static void createTable(Connection connection) {
        String createPurchaseRecordTable = "CREATE TABLE IF NOT EXISTS purchase_record (" +
                "recordID VARCHAR(36) PRIMARY KEY," +
                "marketItemID VARCHAR(36) NOT NULL," +
                "buyerID VARCHAR(36) NOT NULL," +
                "lastPurchaseTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "purchasedAmount INT NOT NULL," +
                "FOREIGN KEY (marketItemID) REFERENCES market_item(MarketItemID)," +
                "FOREIGN KEY (buyerID) REFERENCES player_info(uuid)" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPurchaseRecordTable);
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Create purchase_record table failed.", e);
        }
    }

    public static void insertPurchaseRecord(Connection connection, UUID recordID, UUID marketItemID,
                                            UUID buyerID, int purchasedAmount) {
        String sql = "INSERT INTO purchase_record (recordID, marketItemID, buyerID, lastPurchaseTime, purchasedAmount) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP(), ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, recordID.toString());
            pstmt.setString(2, marketItemID.toString());
            pstmt.setString(3, buyerID.toString());
            pstmt.setInt(4, purchasedAmount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Insert purchase record failed.", e);
        }
    }

    public static Timestamp getLastPurchaseTime(Connection connection, UUID marketItemID, UUID buyerID) {
        String sql = "SELECT MAX(lastPurchaseTime) AS lastTime FROM purchase_record " +
                "WHERE marketItemID = ? AND buyerID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, marketItemID.toString());
            pstmt.setString(2, buyerID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp("lastTime");
            }
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Get last purchase time failed.", e);
        }
        return null;
    }
}
