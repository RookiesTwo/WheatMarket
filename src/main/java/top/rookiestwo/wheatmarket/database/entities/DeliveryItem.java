package top.rookiestwo.wheatmarket.database.entities;

import net.minecraft.nbt.CompoundTag;

import java.sql.Timestamp;
import java.util.UUID;

public class DeliveryItem {
    private UUID deliveryID;
    private UUID receiverID;
    private UUID marketItemID;
    private UUID sourcePlayerID;
    private String itemID;
    private CompoundTag itemNBTCompound;
    private int amount;
    private int remainingAmount;
    private Timestamp createdTime;
    private Timestamp claimedTime;

    public DeliveryItem() {
    }

    public DeliveryItem(UUID deliveryID, UUID receiverID, UUID marketItemID, UUID sourcePlayerID,
                        String itemID, CompoundTag itemNBTCompound, int amount, int remainingAmount,
                        Timestamp createdTime, Timestamp claimedTime) {
        this.deliveryID = deliveryID;
        this.receiverID = receiverID;
        this.marketItemID = marketItemID;
        this.sourcePlayerID = sourcePlayerID;
        this.itemID = itemID;
        this.itemNBTCompound = itemNBTCompound;
        this.amount = amount;
        this.remainingAmount = remainingAmount;
        this.createdTime = createdTime;
        this.claimedTime = claimedTime;
    }

    public UUID getDeliveryID() {
        return deliveryID;
    }

    public void setDeliveryID(UUID deliveryID) {
        this.deliveryID = deliveryID;
    }

    public UUID getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(UUID receiverID) {
        this.receiverID = receiverID;
    }

    public UUID getMarketItemID() {
        return marketItemID;
    }

    public void setMarketItemID(UUID marketItemID) {
        this.marketItemID = marketItemID;
    }

    public UUID getSourcePlayerID() {
        return sourcePlayerID;
    }

    public void setSourcePlayerID(UUID sourcePlayerID) {
        this.sourcePlayerID = sourcePlayerID;
    }

    public String getItemID() {
        return itemID;
    }

    public void setItemID(String itemID) {
        this.itemID = itemID;
    }

    public CompoundTag getItemNBTCompound() {
        return itemNBTCompound;
    }

    public void setItemNBTCompound(CompoundTag itemNBTCompound) {
        this.itemNBTCompound = itemNBTCompound;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(int remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public Timestamp getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Timestamp createdTime) {
        this.createdTime = createdTime;
    }

    public Timestamp getClaimedTime() {
        return claimedTime;
    }

    public void setClaimedTime(Timestamp claimedTime) {
        this.claimedTime = claimedTime;
    }

    @Override
    public String toString() {
        return "DeliveryItem{" +
                "deliveryID=" + deliveryID +
                ", receiverID=" + receiverID +
                ", marketItemID=" + marketItemID +
                ", sourcePlayerID=" + sourcePlayerID +
                ", itemID='" + itemID + '\'' +
                ", itemNBTCompound=" + itemNBTCompound +
                ", amount=" + amount +
                ", remainingAmount=" + remainingAmount +
                ", createdTime=" + createdTime +
                ", claimedTime=" + claimedTime +
                '}';
    }
}
