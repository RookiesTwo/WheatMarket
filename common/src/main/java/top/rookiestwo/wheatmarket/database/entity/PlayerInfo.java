package top.rookiestwo.wheatmarket.database.entity;

import java.util.UUID;

/**
 * 玩家信息实体类 - 纯数据对象
 */
public class PlayerInfo {
    private UUID uuid;
    private Double balance;

    public PlayerInfo(UUID uuid, Double balance) {
        this.uuid = uuid;
        this.balance = balance;
    }

    // Getter和Setter
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "PlayerInfo{" +
                "uuid=" + uuid +
                ", balance=" + balance +
                '}';
    }
}
