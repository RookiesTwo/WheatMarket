package top.rookiestwo.wheatmarket.database;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.caches.MarketItemCache;
import top.rookiestwo.wheatmarket.database.tables.MarketItemTable;
import top.rookiestwo.wheatmarket.database.tables.PlayerInfoTable;
import top.rookiestwo.wheatmarket.database.tables.PurchaseRecordTable;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class WheatMarketDatabase {
    private static final String serverDatabaseUrl = "jdbc:h2:file:./config/WheatMarket/database/WheatMarketDB";
    private static String clientDatabaseUrl = null;
    private Connection connection;
    private MarketItemCache marketItemCache;

    public WheatMarketDatabase(Dist environment) {
        WheatMarket.LOGGER.info("Initializing Database...");
        if (environment == Dist.DEDICATED_SERVER) serverInitialize();
        else clientInitialize();
    }

    private void clientInitialize() {
        //获取存档路径
        Path levelPath = Minecraft.getInstance().getSingleplayerServer().getWorldPath(LevelResource.ROOT);
        String levelName = levelPath.getName(levelPath.getNameCount() - 2).toString();
        clientDatabaseUrl = "jdbc:h2:file:./saves/" + levelName + "/WheatMarketDB";
        connection = openConnection(clientDatabaseUrl);
        createTables();
        WheatMarket.LOGGER.info("Database initialized.");
    }

    public void serverInitialize() {
        connection = openConnection(serverDatabaseUrl);
        createTables();
        WheatMarket.LOGGER.info("Database initialized.");
    }

    private Connection openConnection(String databaseUrl) {
        try {
            return DriverManager.getConnection(databaseUrl);
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Database connection failed.", e);
            throw new IllegalStateException("Failed to connect to database: " + databaseUrl, e);
        }
    }

    private void createTables() {
        PlayerInfoTable.createTable(connection);
        MarketItemTable.createTable(connection);
        PurchaseRecordTable.createTable(connection);
        this.marketItemCache = new MarketItemCache(connection);
    }

    public Connection getConnection() {
        return connection;
    }

    public MarketItemCache getMarketItemCache() {
        return marketItemCache;
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
