package top.rookiestwo.wheatmarket.database.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

public class TransactionManager {
    private final Connection connection;
    private final ExecutorService executor;

    public TransactionManager(Connection connection, ExecutorService executor) {
        this.connection = connection;
        this.executor = executor;
    }

    public <T> CompletableFuture<T> executeAsync(TransactionCallback<T> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(callback);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    private <T> T execute(TransactionCallback<T> callback) throws Exception {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = callback.execute(connection);
            connection.commit();
            return result;
        } catch (Exception e) {
            rollbackQuietly();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }
}
