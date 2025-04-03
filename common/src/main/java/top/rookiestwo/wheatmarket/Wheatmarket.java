package top.rookiestwo.wheatmarket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Wheatmarket {
    public static final String MOD_ID = "wheatmarket";
    public static final Wheatmarket INSTANCE = null;
    public static WheatMarketRegistry REGISTRY = null;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init() {
        Wheatmarket.LOGGER.info("WheatMarket Initializing...");
        // Write common init code here.
        REGISTRY=new WheatMarketRegistry();
    }
}