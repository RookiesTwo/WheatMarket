package top.rookiestwo.wheatmarket.database.entity;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * 购买记录实体类 - 纯数据对象
 */
public class PurchaseRecord {
    private UUID recordID;
    private UUID marketItemID;
    private UUID buyerID;
    private Timestamp lastPurchaseTime;
    private int purchasedAmount;

    public PurchaseRecord(UUID recordID, UUID marketItemID, UUID buyerID,
                          Timestamp lastPurchaseTime, int purchasedAmount) {
        this.recordID = recordID;
        this.marketItemID = marketItemID;
        this.buyerID = buyerID;
        this.lastPurchaseTime = lastPurchaseTime;
        this.purchasedAmount = purchasedAmount;
    }

    // Getter和Setter方法
    public UUID getRecordID() {
        return recordID;
    }

    public void setRecordID(UUID recordID) {
        this.recordID = recordID;
    }

    public UUID getMarketItemID() {
        return marketItemID;
    }

    public void setMarketItemID(UUID marketItemID) {
        this.marketItemID = marketItemID;
    }

    public UUID getBuyerID() {
        return buyerID;
    }

    public void setBuyerID(UUID buyerID) {
        this.buyerID = buyerID;
    }

    public Timestamp getLastPurchaseTime() {
        return lastPurchaseTime;
    }

    public void setLastPurchaseTime(Timestamp lastPurchaseTime) {
        this.lastPurchaseTime = lastPurchaseTime;
    }

    public int getPurchasedAmount() {
        return purchasedAmount;
    }

    public void setPurchasedAmount(int purchasedAmount) {
        this.purchasedAmount = purchasedAmount;
    }

    @Override
    public String toString() {
        return "PurchaseRecord{" +
                "recordID=" + recordID +
                ", marketItemID=" + marketItemID +
                ", buyerID=" + buyerID +
                ", lastPurchaseTime=" + lastPurchaseTime +
                ", purchasedAmount=" + purchasedAmount +
                '}';
    }
}
