package com.autumn.jdbc.tx;

import java.sql.Connection;

/**
 * @author huangcanjie
 */
public class TransactionalUtils {

    public static Connection getCurrentConnection() {
        TransactionStatus status = DataSourceTransactionManager.transactionStatus.get();
        return status == null ? null : status.connection;
    }
}
