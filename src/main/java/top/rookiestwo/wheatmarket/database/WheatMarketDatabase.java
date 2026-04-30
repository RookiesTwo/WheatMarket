package top.rookiestwo.wheatmarket.database;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.caches.MarketItemCache;
import top.rookiestwo.wheatmarket.database.repository.DeliveryItemRepository;
import top.rookiestwo.wheatmarket.database.repository.MarketItemRepository;
import top.rookiestwo.wheatmarket.database.repository.PlayerInfoRepository;
import top.rookiestwo.wheatmarket.database.repository.PurchaseRecordRepository;
import top.rookiestwo.wheatmarket.database.transaction.TransactionManager;
import top.rookiestwo.wheatmarket.service.DeliveryService;
import top.rookiestwo.wheatmarket.service.EconomyService;
import top.rookiestwo.wheatmarket.service.MarketService;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WheatMarketDatabase {
    private static final String serverDatabaseUrl = "jdbc:h2:file:./config/WheatMarket/database/WheatMarketDB";
    private static String clientDatabaseUrl = null;
    private Connection connection;
    private MarketItemCache marketItemCache;
    private ExecutorService dbExecutor;
    private TransactionManager transactionManager;
    private PlayerInfoRepository playerInfoRepository;
    private MarketItemRepository marketItemRepository;
    private PurchaseRecordRepository purchaseRecordRepository;
    private DeliveryItemRepository deliveryItemRepository;
    private EconomyService economyService;
    private DeliveryService deliveryService;
    private MarketService marketService;

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
        dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "WheatMarket-DB");
            thread.setDaemon(true);
            return thread;
        });
        transactionManager = new TransactionManager(connection, dbExecutor);
        playerInfoRepository = new PlayerInfoRepository();
        marketItemRepository = new MarketItemRepository();
        purchaseRecordRepository = new PurchaseRecordRepository();
        deliveryItemRepository = new DeliveryItemRepository();

        try {
            playerInfoRepository.createTable(connection);
            marketItemRepository.createTable(connection);
            purchaseRecordRepository.createTable(connection);
            deliveryItemRepository.createTable(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create WheatMarket database tables", e);
        }

        this.marketItemCache = new MarketItemCache(connection);
        economyService = new EconomyService(transactionManager, playerInfoRepository);
        deliveryService = new DeliveryService(transactionManager, deliveryItemRepository);
        marketService = new MarketService(transactionManager, playerInfoRepository, marketItemRepository,
                purchaseRecordRepository, deliveryItemRepository, marketItemCache);
    }

    public MarketItemCache getMarketItemCache() {
        return marketItemCache;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public DeliveryService getDeliveryService() {
        return deliveryService;
    }

    public MarketService getMarketService() {
        return marketService;
    }

    public void closeConnection() {
        if (dbExecutor != null) {
            dbExecutor.shutdown();
            try {
                if (!dbExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    dbExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                dbExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Failed to close database connection.", e);
        }
    }
}
