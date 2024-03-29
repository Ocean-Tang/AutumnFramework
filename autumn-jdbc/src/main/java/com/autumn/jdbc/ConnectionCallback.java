package com.autumn.jdbc;

import com.sun.istack.internal.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 由上层代码指定如何处理 Connection
 * @param <T>
 */
@FunctionalInterface
public interface ConnectionCallback<T>{

    @Nullable
    T doInConnection(Connection conn) throws SQLException;
}
