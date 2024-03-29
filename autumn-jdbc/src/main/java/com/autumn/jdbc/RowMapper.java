package com.autumn.jdbc;

import com.sun.istack.internal.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author huangcanjie
 */

/**
 * 由上层代码决定如何将 PreparedStatement 的查询结果进行映射
 * @param <T>
 */
@FunctionalInterface
public interface RowMapper<T> {

    @Nullable
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
