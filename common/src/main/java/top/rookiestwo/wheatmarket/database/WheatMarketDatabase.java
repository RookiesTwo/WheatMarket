package top.rookiestwo.wheatmarket.database;

import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import top.rookiestwo.wheatmarket.WheatMarket;
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

    public WheatMarketDatabase(Env environment) {
        WheatMarket.LOGGER.info("Initializing Database...");
        if (environment == Env.SERVER) serverInitialize();
        else clientInitialize();
    }

    private void clientInitialize() {
        //获取存档路径
        Path levelPath = Minecraft.getInstance().getSingleplayerServer().getWorldPath(LevelResource.ROOT);
        String levelName = levelPath.getName(levelPath.getNameCount() - 2).toString();
        clientDatabaseUrl = "jdbc:h2:file:./saves/" + levelName + "/WheatMarketDB";
        try {
            connection = DriverManager.getConnection(clientDatabaseUrl);
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Database connection failed.", e);
        }
        createTables();
        WheatMarket.LOGGER.info("Database initialized.");
    }

    public void serverInitialize() {
        try {
            connection = DriverManager.getConnection(serverDatabaseUrl);
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Database connection failed.", e);
        }
        createTables();
        WheatMarket.LOGGER.info("Database initialized.");
    }

    private void createTables() {
        PlayerInfoTable.createTable(connection);
        MarketItemTable.createTable(connection);
        PurchaseRecordTable.createTable(connection);
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