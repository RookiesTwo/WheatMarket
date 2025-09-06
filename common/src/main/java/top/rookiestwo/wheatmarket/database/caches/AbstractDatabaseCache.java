package top.rookiestwo.wheatmarket.database.caches;

import java.sql.Connection;

public abstract class AbstractDatabaseCache {

    abstract void loadAllFromDatabase(Connection connection);

    abstract void saveAllToDatabase(Connection connection);
}