package top.rookiestwo.wheatmarket.database;

import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.tables.MarketItem;
import top.rookiestwo.wheatmarket.database.tables.PlayerInfo;
import top.rookiestwo.wheatmarket.database.tables.PurchaseRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class WheatMarketDatabase {
    private static final String databaseUrl = "jdbc:h2:file:./config/WheatMarket/database/WheatMarketDB";
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
        PlayerInfo.createTable(connection);
        MarketItem.createTable(connection);
        PurchaseRecord.createTable(connection);
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

