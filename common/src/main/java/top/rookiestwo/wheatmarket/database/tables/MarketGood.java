package top.rookiestwo.wheatmarket.database.tables;

import java.sql.Timestamp;
import java.util.UUID;

public class MarketGood {
    private UUID listingID;
    private String itemID;
    private String classification;
    private UUID sellerID;
    private Double price;
    private int amount;
    private Timestamp listingTime;
    private Boolean ifAdmin;
    private Boolean ifSell;
    private String BuyerTimerJson;
    private int CooldownAmount;
    private int CooldownTimeInMinutes;
    private int daysToExpire;
    private Timestamp lastTradeTime;
}
