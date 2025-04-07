package top.rookiestwo.wheatmarket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.rookiestwo.wheatmarket.command.WheatMarketCommands;
import top.rookiestwo.wheatmarket.database.WheatMarketDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WheatMarket {
    public static final String MOD_ID = "wheatmarket";
    public static final WheatMarket INSTANCE = null;
    public static WheatMarketRegistry REGISTRY = null;
    public static final Logger LOGGER = LogManager.getLogger("Wheat Market");
    public static WheatMarketDatabase DATABASE = null;
    public static WheatMarketCommands WHEAT_MARKET_COMMANDS = null;
    public static ExecutorService ASYNC = null;

    public static void init() {
        WheatMarket.LOGGER.info("WheatMarket Initializing...");
        // Write common init code here.
        WHEAT_MARKET_COMMANDS = new WheatMarketCommands();
        WheatMarketRegistry.registerEvents();
        REGISTRY=new WheatMarketRegistry();
    }
}