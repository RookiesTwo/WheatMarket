package top.rookiestwo.wheatmarket.service;

import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.repository.PlayerInfoRepository;
import top.rookiestwo.wheatmarket.database.transaction.TransactionManager;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyService {
    private final TransactionManager transactionManager;
    private final PlayerInfoRepository playerInfoRepository;

    public EconomyService(TransactionManager transactionManager, PlayerInfoRepository playerInfoRepository) {
        this.transactionManager = transactionManager;
        this.playerInfoRepository = playerInfoRepository;
    }

    public CompletableFuture<ServiceResult<Void>> ensurePlayerRecord(UUID playerId) {
        return transactionManager.executeAsync(connection -> {
            playerInfoRepository.ensureExists(connection, playerId);
            return ServiceResult.<Void>success(null);
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to ensure player economy record.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<Double>> getBalance(UUID playerId) {
        return transactionManager.executeAsync(connection -> {
            playerInfoRepository.ensureExists(connection, playerId);
            return ServiceResult.success(playerInfoRepository.getBalance(connection, playerId));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to get player balance.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<Double>> addBalance(UUID playerId, double amount) {
        if (!isPositiveMoney(amount)) {
            return CompletableFuture.completedFuture(ServiceResult.<Double>failure("gui.wheatmarket.operation.invalid_amount"));
        }
        return transactionManager.executeAsync(connection -> {
            playerInfoRepository.ensureExists(connection, playerId);
            double currentBalance = playerInfoRepository.getBalance(connection, playerId);
            double newBalance = currentBalance + amount;
            if (!isNonNegativeMoney(newBalance)) {
                return ServiceResult.<Double>failure("gui.wheatmarket.operation.invalid_amount");
            }
            playerInfoRepository.setBalance(connection, playerId, newBalance);
            return ServiceResult.success(newBalance);
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to add player balance.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<Double>> setBalance(UUID playerId, double amount) {
        if (!isNonNegativeMoney(amount)) {
            return CompletableFuture.completedFuture(ServiceResult.<Double>failure("gui.wheatmarket.operation.invalid_amount"));
        }
        return transactionManager.executeAsync(connection -> {
            playerInfoRepository.ensureExists(connection, playerId);
            playerInfoRepository.setBalance(connection, playerId, amount);
            return ServiceResult.success(amount);
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to set player balance.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<Double>> removeBalance(UUID playerId, double amount) {
        if (!isPositiveMoney(amount)) {
            return CompletableFuture.completedFuture(ServiceResult.<Double>failure("gui.wheatmarket.operation.invalid_amount"));
        }
        return transactionManager.executeAsync(connection -> {
            playerInfoRepository.ensureExists(connection, playerId);
            double balance = playerInfoRepository.getBalance(connection, playerId);
            if (amount > balance) {
                return ServiceResult.<Double>failure("error.command.wheatmarket.admin_remove_balance", String.valueOf(balance));
            }
            double newBalance = balance - amount;
            if (!isNonNegativeMoney(newBalance)) {
                return ServiceResult.<Double>failure("gui.wheatmarket.operation.invalid_amount");
            }
            playerInfoRepository.setBalance(connection, playerId, newBalance);
            return ServiceResult.success(newBalance);
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to remove player balance.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<TransferResult>> transfer(UUID senderId, UUID targetId, double amount) {
        if (!isPositiveMoney(amount)) {
            return CompletableFuture.completedFuture(ServiceResult.<TransferResult>failure("gui.wheatmarket.operation.invalid_amount"));
        }
        return transactionManager.executeAsync(connection -> {
            playerInfoRepository.ensureExists(connection, senderId);
            playerInfoRepository.ensureExists(connection, targetId);
            double senderBalance = playerInfoRepository.getBalance(connection, senderId);
            if (senderBalance < amount) {
                return ServiceResult.<TransferResult>failure("info.command.wheatmarket.not_enough_money");
            }
            double targetBalance = playerInfoRepository.getBalance(connection, targetId);
            double newSenderBalance = senderBalance - amount;
            double newTargetBalance = targetBalance + amount;
            if (!isNonNegativeMoney(newSenderBalance) || !isNonNegativeMoney(newTargetBalance)) {
                return ServiceResult.<TransferResult>failure("gui.wheatmarket.operation.invalid_amount");
            }

            playerInfoRepository.setBalance(connection, senderId, newSenderBalance);
            playerInfoRepository.setBalance(connection, targetId, newTargetBalance);
            return ServiceResult.success(new TransferResult(newSenderBalance, newTargetBalance));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to transfer balance.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    private boolean isPositiveMoney(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }

    private boolean isNonNegativeMoney(double amount) {
        return Double.isFinite(amount) && amount >= 0;
    }

    public record TransferResult(double senderBalance, double targetBalance) {
    }
}
