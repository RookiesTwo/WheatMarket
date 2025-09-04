package top.rookiestwo.wheatmarket.database.entity;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * 市场商品实体类 - 纯数据对象
 * 表示市场中的一个商品条目
 */
public class MarketItem {
    private UUID marketItemID;
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

    // 默认构造函数
    public MarketItem() {
    }

    // 完整参数构造函数
    public MarketItem(UUID marketItemID, String itemID, UUID sellerID, Double price,
                      int amount, Timestamp listingTime, Boolean ifAdmin, Boolean ifSell,
                      int cooldownAmount, int cooldownTimeInMinutes, long timeToExpire,
                      Timestamp lastTradeTime) {
        this.marketItemID = marketItemID;
        this.itemID = itemID;
        this.sellerID = sellerID;
        this.price = price;
        this.amount = amount;
        this.listingTime = listingTime;
        this.ifAdmin = ifAdmin;
        this.ifSell = ifSell;
        this.cooldownAmount = cooldownAmount;
        this.cooldownTimeInMinutes = cooldownTimeInMinutes;
        this.timeToExpire = timeToExpire;
        this.lastTradeTime = lastTradeTime;
    }

    // 快速创建构造函数（常用字段）
    public MarketItem(String itemID, UUID sellerID, Double price, int amount, Boolean ifAdmin) {
        this.marketItemID = UUID.randomUUID();
        this.itemID = itemID;
        this.sellerID = sellerID;
        this.price = price;
        this.amount = amount;
        this.listingTime = new Timestamp(System.currentTimeMillis());
        this.ifAdmin = ifAdmin;
        this.ifSell = true;
        this.cooldownAmount = 0;
        this.cooldownTimeInMinutes = 0;
        this.timeToExpire = 0;
        this.lastTradeTime = null;
    }

    // Getter和Setter方法
    public UUID getMarketItemID() {
        return marketItemID;
    }

    public void setMarketItemID(UUID marketItemID) {
        this.marketItemID = marketItemID;
    }

    public String getItemID() {
        return itemID;
    }

    public void setItemID(String itemID) {
        this.itemID = itemID;
    }

    public UUID getSellerID() {
        return sellerID;
    }

    public void setSellerID(UUID sellerID) {
        this.sellerID = sellerID;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Timestamp getListingTime() {
        return listingTime;
    }

    public void setListingTime(Timestamp listingTime) {
        this.listingTime = listingTime;
    }

    public Boolean getIfAdmin() {
        return ifAdmin;
    }

    public void setIfAdmin(Boolean ifAdmin) {
        this.ifAdmin = ifAdmin;
    }

    public Boolean getIfSell() {
        return ifSell;
    }

    public void setIfSell(Boolean ifSell) {
        this.ifSell = ifSell;
    }

    public int getCooldownAmount() {
        return cooldownAmount;
    }

    public void setCooldownAmount(int cooldownAmount) {
        this.cooldownAmount = cooldownAmount;
    }

    public int getCooldownTimeInMinutes() {
        return cooldownTimeInMinutes;
    }

    public void setCooldownTimeInMinutes(int cooldownTimeInMinutes) {
        this.cooldownTimeInMinutes = cooldownTimeInMinutes;
    }

    public long getTimeToExpire() {
        return timeToExpire;
    }

    public void setTimeToExpire(long timeToExpire) {
        this.timeToExpire = timeToExpire;
    }

    public Timestamp getLastTradeTime() {
        return lastTradeTime;
    }

    public void setLastTradeTime(Timestamp lastTradeTime) {
        this.lastTradeTime = lastTradeTime;
    }

    // 实用方法

    /**
     * 检查商品是否已过期
     */
    public boolean isExpired() {
        if (timeToExpire <= 0) {
            return false; // 永不过期
        }
        return System.currentTimeMillis() > (listingTime.getTime() + timeToExpire);
    }

    /**
     * 获取商品总价值
     */
    public double getTotalValue() {
        return price * amount;
    }

    /**
     * 检查是否有冷却限制
     */
    public boolean hasCooldownRestriction() {
        return cooldownTimeInMinutes > 0 || cooldownAmount > 0;
    }

    /**
     * 检查商品是否可用（在售且未过期）
     */
    public boolean isAvailable() {
        return ifSell && !isExpired() && amount > 0;
    }

    /**
     * 减少商品数量
     */
    public boolean reduceAmount(int reduceBy) {
        if (reduceBy <= 0 || reduceBy > amount) {
            return false;
        }
        this.amount -= reduceBy;
        this.lastTradeTime = new Timestamp(System.currentTimeMillis());
        return true;
    }

    @Override
    public String toString() {
        return "MarketItem{" +
                "marketItemID=" + marketItemID +
                ", itemID='" + itemID + '\'' +
                ", sellerID=" + sellerID +
                ", price=" + price +
                ", amount=" + amount +
                ", listingTime=" + listingTime +
                ", ifAdmin=" + ifAdmin +
                ", ifSell=" + ifSell +
                ", cooldownAmount=" + cooldownAmount +
                ", cooldownTimeInMinutes=" + cooldownTimeInMinutes +
                ", timeToExpire=" + timeToExpire +
                ", lastTradeTime=" + lastTradeTime +
                ", available=" + isAvailable() +
                ", totalValue=" + getTotalValue() +
                '}';
    }
}
