package com.autumn.jdbc;

import com.sun.istack.internal.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 有上层代码决定如何 执行 PreparedStatement , executeQuery(), executeUpdate()
 * @param <T>
 */
@FunctionalInterface
public interface PreparedStatementCallback<T> {

    @Nullable
    T doInPreparedStatement(PreparedStatement ps) throws SQLException;
}
