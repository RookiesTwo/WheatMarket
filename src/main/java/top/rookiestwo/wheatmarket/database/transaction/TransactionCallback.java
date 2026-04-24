package top.rookiestwo.wheatmarket.database.transaction;

import java.sql.Connection;

@FunctionalInterface
public interface TransactionCallback<T> {
    T execute(Connection connection) throws Exception;
}
