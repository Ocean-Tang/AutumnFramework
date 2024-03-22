package com.autumn.jdbc.tx;

import java.sql.Connection;

/**
 * @author huangcanjie
 */
public class TransactionStatus {

    final Connection connection;

    public TransactionStatus(Connection connection) {
        this.connection = connection;
    }
}
