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
                "listingID VARCHAR(36) PRIMARY KEY," +
                "itemID VARCHAR(255) NOT NULL," +
                "classification VARCHAR(255) NOT NULL," +
                "sellerID VARCHAR(36) NOT NULL," +
                "price DOUBLE NOT NULL," +
                "amount INT NOT NULL," +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (sellerID) REFERENCES player_info(uuid)" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createMarketItemTable);
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Create tables failed.", e);
        }
    }
}