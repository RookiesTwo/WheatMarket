package top.rookiestwo.wheatmarket.database.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class PlayerInfoRepository {
    public void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS player_info (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "balance DOUBLE" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void ensureExists(Connection connection, UUID uuid) throws SQLException {
        if (!exists(connection, uuid)) {
            insert(connection, uuid, 0.0);
        }
    }

    public void insert(Connection connection, UUID uuid, double balance) throws SQLException {
        String sql = "INSERT INTO player_info (uuid, balance) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, balance);
            stmt.executeUpdate();
        }
    }

    public void setBalance(Connection connection, UUID uuid, double balance) throws SQLException {
        String sql = "UPDATE player_info SET balance = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, balance);
            stmt.setString(2, uuid.toString());
            requireUpdated(stmt.executeUpdate(), "No player row for " + uuid);
        }
    }

    public double getBalance(Connection connection, UUID uuid) throws SQLException {
        String sql = "SELECT balance FROM player_info WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        }
        throw new SQLException("No player row for " + uuid);
    }

    public boolean exists(Connection connection, UUID uuid) throws SQLException {
        String sql = "SELECT 1 FROM player_info WHERE uuid = ? LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    private void requireUpdated(int rows, String message) throws SQLException {
        if (rows == 0) {
            throw new SQLException(message);
        }
    }
}
