package top.rookiestwo.wheatmarket.database.tables;

import java.sql.Timestamp;
import java.util.UUID;

public class MarketItem {
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
}