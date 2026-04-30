package top.rookiestwo.wheatmarket.database.repository;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import top.rookiestwo.wheatmarket.database.entities.DeliveryItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeliveryItemRepository {
    public void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS market_delivery (" +
                "deliveryID VARCHAR(36) PRIMARY KEY," +
                "receiverID VARCHAR(36) NOT NULL," +
                "marketItemID VARCHAR(36)," +
                "sourcePlayerID VARCHAR(36)," +
                "itemID VARCHAR(255) NOT NULL," +
                "itemNBTCompound CLOB," +
                "amount INT NOT NULL," +
                "remainingAmount INT NOT NULL," +
                "createdTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "claimedTime TIMESTAMP," +
                "FOREIGN KEY (receiverID) REFERENCES player_info(uuid)," +
                "FOREIGN KEY (sourcePlayerID) REFERENCES player_info(uuid)" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void insert(Connection connection, DeliveryItem item) throws SQLException {
        String sql = "INSERT INTO market_delivery (deliveryID, receiverID, marketItemID, sourcePlayerID, itemID, " +
                "itemNBTCompound, amount, remainingAmount, createdTime, claimedTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, item.getDeliveryID().toString());
            stmt.setString(2, item.getReceiverID().toString());
            stmt.setString(3, item.getMarketItemID() == null ? null : item.getMarketItemID().toString());
            stmt.setString(4, item.getSourcePlayerID() == null ? null : item.getSourcePlayerID().toString());
            stmt.setString(5, item.getItemID());
            stmt.setString(6, item.getItemNBTCompound() == null ? null : item.getItemNBTCompound().toString());
            stmt.setInt(7, item.getAmount());
            stmt.setInt(8, item.getRemainingAmount());
            stmt.setTimestamp(9, item.getCreatedTime());
            stmt.setTimestamp(10, item.getClaimedTime());
            stmt.executeUpdate();
        }
    }

    public DeliveryItem findById(Connection connection, UUID deliveryId) throws SQLException {
        String sql = "SELECT * FROM market_delivery WHERE deliveryID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, deliveryId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return resultSetToDeliveryItem(rs);
            }
        }
        return null;
    }

    public List<DeliveryItem> findByReceiver(Connection connection, UUID receiverId) throws SQLException {
        String sql = "SELECT * FROM market_delivery WHERE receiverID = ? ORDER BY createdTime ASC, deliveryID ASC";
        List<DeliveryItem> results = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, receiverId.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(resultSetToDeliveryItem(rs));
            }
        }
        return results;
    }

    public void updateRemainingAmount(Connection connection, UUID deliveryId, int remainingAmount) throws SQLException {
        String sql = "UPDATE market_delivery SET remainingAmount = ?, claimedTime = ? WHERE deliveryID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, remainingAmount);
            stmt.setTimestamp(2, remainingAmount <= 0 ? new java.sql.Timestamp(System.currentTimeMillis()) : null);
            stmt.setString(3, deliveryId.toString());
            requireUpdated(stmt.executeUpdate(), "No delivery row for " + deliveryId);
        }
    }

    public void delete(Connection connection, UUID deliveryId) throws SQLException {
        String sql = "DELETE FROM market_delivery WHERE deliveryID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, deliveryId.toString());
            requireUpdated(stmt.executeUpdate(), "No delivery row for " + deliveryId);
        }
    }

    private DeliveryItem resultSetToDeliveryItem(ResultSet rs) throws SQLException {
        UUID deliveryId = UUID.fromString(rs.getString("deliveryID"));
        return new DeliveryItem(
                deliveryId,
                UUID.fromString(rs.getString("receiverID")),
                parseUuid(rs.getString("marketItemID")),
                parseUuid(rs.getString("sourcePlayerID")),
                rs.getString("itemID"),
                parseTag(rs.getString("itemNBTCompound"), deliveryId),
                rs.getInt("amount"),
                rs.getInt("remainingAmount"),
                rs.getTimestamp("createdTime"),
                rs.getTimestamp("claimedTime")
        );
    }

    private UUID parseUuid(String raw) {
        return raw == null || raw.isBlank() ? null : UUID.fromString(raw);
    }

    private CompoundTag parseTag(String raw, UUID deliveryId) throws SQLException {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TagParser.parseTag(raw);
        } catch (Exception e) {
            throw new SQLException("Failed to parse delivery item NBT for " + deliveryId, e);
        }
    }

    private void requireUpdated(int rows, String message) throws SQLException {
        if (rows == 0) {
            throw new SQLException(message);
        }
    }
}
