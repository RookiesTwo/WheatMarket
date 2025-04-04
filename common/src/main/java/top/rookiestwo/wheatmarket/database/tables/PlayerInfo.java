package top.rookiestwo.wheatmarket.database.tables;

import top.rookiestwo.wheatmarket.WheatMarket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.sql.Connection;

public class PlayerInfo {
    private UUID uuid;
    private Double balance;

    public static void insertPlayerInfo(Connection connection,UUID uuid,Double balance) {
        String insertPlayerInfo = "INSERT INTO player_info (uuid, balance) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertPlayerInfo)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setDouble(2, balance);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Insert player info failed.", e);
        }
    }

    public static void updatePlayerBalance(Connection connection, UUID uuid, Double balance) {
        String updatePlayerInfo = "UPDATE player_info SET balance = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updatePlayerInfo)) {
            pstmt.setDouble(1, balance);
            pstmt.setString(2, uuid.toString());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                WheatMarket.LOGGER.warn("Failed to update player with UUID: {}", uuid);
            }
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Update player info failed.", e);
        }
    }

    public static void addPlayerBalance(Connection connection, UUID uuid, Double amount) {
        String addPlayerBalance = "UPDATE player_info SET balance = balance + ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(addPlayerBalance)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, uuid.toString());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                WheatMarket.LOGGER.warn("Failed to add balance for player with UUID: {}", uuid);
            }
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Add player balance failed.", e);
        }
    }

    public static double getPlayerBalance(Connection connection, UUID uuid) {
        String getPlayerBalance = "SELECT balance FROM player_info WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(getPlayerBalance)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                return rs.getDouble("balance");
            } else {
                WheatMarket.LOGGER.warn("No player found with UUID: {}", uuid);
                return 0.0;
            }
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Get player balance failed.", e);
            return 0.0;
        }
    }

    public static boolean uuidExists(Connection connection, UUID uuid) {
        String checkUUID = "SELECT 1 FROM player_info WHERE uuid = ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(checkUUID)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            WheatMarket.LOGGER.error("Check UUID failed.", e);
            return false;
        }
    }

    public static void ifNotExistsCreateRecord(Connection connection,UUID uuid) {
        if(WheatMarket.DATABASE==null){
            WheatMarket.LOGGER.error("Database is null");
            return;
        }
        if(!uuidExists(connection,uuid)){
            WheatMarket.LOGGER.info("Creating player record with UUID: {}", uuid);
            insertPlayerInfo(connection,uuid,0.0);
        }
    }
}