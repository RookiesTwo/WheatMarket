package top.rookiestwo.wheatmarket.database.tables;

import java.sql.Timestamp;
import java.util.UUID;

public class MarketItem {
    private String itemID;
    private UUID sellerUUID;
    private Float price;
    private int amount;
    private Timestamp timestamp;
}
