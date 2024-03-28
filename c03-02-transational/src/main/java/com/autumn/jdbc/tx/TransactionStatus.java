package com.autumn.jdbc.tx;

import java.sql.Connection;

/**
 * 记录当前事务的状态，保存当前的数据库连接，后续可以在这里面存储事务传播机制
 * @author huangcanjie
 */
public class TransactionStatus {

    final Connection connection;

    public TransactionStatus(Connection connection) {
        this.connection = connection;
    }
}
