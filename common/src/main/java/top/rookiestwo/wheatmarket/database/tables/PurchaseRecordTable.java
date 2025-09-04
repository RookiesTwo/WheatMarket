package top.rookiestwo.wheatmarket.database.tables;

import top.rookiestwo.wheatmarket.WheatMarket;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

public class PurchaseRecordTable {
    private UUID recordID;
    private UUID marketItemID;  // 外键关联MarketItem
    private UUID buyerID;       // 外键关联PlayerInfo
    private Timestamp lastPurchaseTime;
    private int purchasedAmount;

    public static void createTable(Connection connection) {
        String createPurchaseRecordTable = "CREATE TABLE IF NOT EXISTS purchase_record (" +
                "recordID VARCHAR(36) PRIMARY KEY," +
                "marketItemID VARCHAR(36) NOT NULL," +
                "buyerID VARCHAR(36) NOT NULL," +
                "lastPurchaseTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "purchasedAmount INT NOT NULL," +
                "FOREIGN KEY (marketItemID) REFERENCES market_item(listingID)," +
                "FOREIGN KEY (buyerID) REFERENCES player_info(uuid)" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPurchaseRecordTable);
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Create purchase_record table failed.", e);
        }
    }
}
