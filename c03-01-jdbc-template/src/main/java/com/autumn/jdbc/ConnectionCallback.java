package com.autumn.jdbc;

import com.sun.istack.internal.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionCallback<T>{

    @Nullable
    T doInConnection(Connection conn) throws SQLException;
}
