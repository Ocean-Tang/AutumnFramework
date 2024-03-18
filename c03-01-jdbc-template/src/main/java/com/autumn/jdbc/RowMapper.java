package com.autumn.jdbc;

import com.sun.istack.internal.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author huangcanjie
 */
@FunctionalInterface
public interface RowMapper<T> {

    @Nullable
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
