package top.rookiestwo.wheatmarket;

import net.neoforged.bus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.rookiestwo.wheatmarket.database.WheatMarketDatabase;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.s2c.DeliveryListS2CPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

import java.util.List;

public final class WheatMarket {
    public static final String MOD_ID = "wheatmarket";
    public static WheatMarketRegistry REGISTRY = null;
    public static final Logger LOGGER = LogManager.getLogger("Wheat Market");
    public static WheatMarketDatabase DATABASE = null;

    public static List<MarketListS2CPacket.MarketItemSummary> CLIENT_MARKET_LIST = null;
    public static int CLIENT_TOTAL_PAGES = 0;
    public static int CLIENT_CURRENT_PAGE = 0;
    public static int CLIENT_MARKET_LIST_VERSION = 0;
    public static List<DeliveryListS2CPacket.DeliverySummary> CLIENT_DELIVERY_LIST = null;
    public static int CLIENT_DELIVERY_TOTAL_PAGES = 0;
    public static int CLIENT_DELIVERY_CURRENT_PAGE = 0;
    public static int CLIENT_DELIVERY_LIST_VERSION = 0;
    public static double CLIENT_BALANCE = 0.0;

    public static void init(IEventBus modBus) {
        WheatMarket.LOGGER.info("WheatMarket Initializing...");
        REGISTRY = new WheatMarketRegistry(modBus);
        WheatMarketNetwork.init(modBus);
    }
}
