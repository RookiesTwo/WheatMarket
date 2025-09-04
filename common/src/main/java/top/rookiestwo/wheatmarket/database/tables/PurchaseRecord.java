package top.rookiestwo.wheatmarket.database.tables;

import java.sql.Timestamp;
import java.util.UUID;

public class PurchaseRecord {
    private UUID recordID;
    private UUID marketItemID;  // 外键关联MarketItem
    private UUID buyerID;       // 外键关联PlayerInfo
    private Timestamp lastPurchaseTime;
    private int purchasedAmount;
}
