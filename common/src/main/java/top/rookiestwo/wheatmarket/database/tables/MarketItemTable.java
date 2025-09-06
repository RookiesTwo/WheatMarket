package top.rookiestwo.wheatmarket.database.tables;

import top.rookiestwo.wheatmarket.WheatMarket;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

public class MarketItemTable {
    private UUID MarketItemID;
    private String itemID;
    private String itemNBTCompound;
    private UUID sellerID;
    private Double price;
    private int amount;
    private Timestamp listingTime;
    private Boolean ifAdmin;
    private Boolean ifSell;
    private int cooldownAmount;
    private int cooldownTimeInMinutes;
    private long timeToExpire;
    private Timestamp lastTradeTime;

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
}