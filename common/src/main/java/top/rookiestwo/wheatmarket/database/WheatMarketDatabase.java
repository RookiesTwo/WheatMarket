package top.rookiestwo.wheatmarket.database;

import top.rookiestwo.wheatmarket.WheatMarket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class WheatMarketDatabase {
    private static final String databaseUrl = "jdbc:h2:file:./config/WheatMarket/database";
    private Connection connection;

    public WheatMarketDatabase() {
        WheatMarket.LOGGER.info("Initializing Database...");
        initialize();
    }

    public void initialize() {
        try {
            connection = DriverManager.getConnection(databaseUrl);
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Database connection failed.", e);
        }
        createTables();
    }

    private void createTables() {
        String createPlayerInfoTable = "CREATE TABLE IF NOT EXISTS player_info (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(255)," +
                "balance DOUBLE" +
                ");";
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
            stmt.execute(createPlayerInfoTable);
            stmt.execute(createMarketItemTable);
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Create tables failed.", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Failed to close database connection.", e);
        }
    }
}

