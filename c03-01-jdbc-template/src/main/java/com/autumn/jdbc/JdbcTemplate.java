package com.autumn.jdbc;

import com.autumn.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author huangcanjie
 */
public class JdbcTemplate {

    final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Number queryForNumber(String sql, Object... args) {
        return queryForObject(sql, NumberRowMapper.instance, args);
    }

    public <T> T queryForObject(String sql, Class<T> clazz, Object... args) {
        if (clazz == String.class) {
            return (T) queryForObject(sql, StringRowMapper.instance, args);
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) queryForObject(sql, BooleanRowMapper.instance, args);
        }
        if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive()) {
            return (T) queryForObject(sql, NumberRowMapper.instance, args);
        }
        return queryForObject(sql, new BeanRowMapper<>(clazz), args);
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        return execute(preparedStatementCreator(sql, args),
                // PreparedStatementCallback
                (PreparedStatement ps) -> {
                    T t = null;
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (t == null) {
                                t = rowMapper.mapRow(rs, rs.getRow());
                            } else {
                                throw new DataAccessException("Multiple rows found.");
                            }
                        }
                    }
                    if (t == null) {
                        throw new DataAccessException("Empty result set.");
                    }
                    return t;
                });
    }

    public <T> List<T> queryForList(String sql, Class<T> clazz, Object... args) {
        return queryForList(sql, new BeanRowMapper<>(clazz), args);
    }

    public <T> List<T> queryForList(String sql, RowMapper<T> rowMapper, Object... args) {
        return execute(preparedStatementCreator(sql, args),
                (ps) -> {
                    List<T> list = new ArrayList<>();
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(rowMapper.mapRow(rs, rs.getRow()));
                        }
                    }
                    return list;
                }
        );
    }

    /*
    执行 update 相关语句，并返回可以搜索的主键
     */
    public Number updateAndReturnGeneratedKey(String sql, Object... args) {
        return execute(
                // 回调方法为，返回一个预编译语句，执行成功后会返回可以搜索的主键
                (conn -> {
                    PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    bindArgs(ps, args);
                    return ps;
                }),
                // 回调方法为，执行 PreparedStatement 的 executeUpdate
                ps -> {
                    int n = ps.executeUpdate();
                    if (n == 0) {
                        throw new DataAccessException("0 rows inserted.");
                    }
                    if (n > 1) {
                        throw new DataAccessException("Multiple rows inserted.");
                    }
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        while (rs.next()) {
                            return (Number) rs.getObject(1);
                        }
                    }
                    throw new DataAccessException("Should not reach here.");
                }
        );
    }

    public int update(String sql, Object... args) {
        // 传递预编译创造器，其中回调方法为得到一个预编译好的 PreparedStatement
        // 传递的回调方法为，执行传递的 PreparedStatement 的 executeUpdate 方法
        return execute(preparedStatementCreator(sql, args),
                PreparedStatement::executeUpdate);
    }

    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) {
        return execute(conn -> {
            // execute，回调方法为，执行 psc 中的回调方法，得到预编译的sql语句，然后执行 action 的回调方法，执行其中sql语句
            try (PreparedStatement ps = psc.createPreparedStatement(conn)) {
                return action.doInPreparedStatement(ps);
            }
        });
    }

    /*
    执行 connection 接口传递进来的回调方法
     */
    public <T> T execute(ConnectionCallback<T> action) {
        try (Connection conn = dataSource.getConnection()) {
            // 检查是否开启自动提交，如果没有，则设置自动提交，连接处理完毕后，再重置
            final boolean autoCommit = conn.getAutoCommit();
            if (!autoCommit) {
                conn.setAutoCommit(true);
            }
            T result = action.doInConnection(conn);
            if (!autoCommit) {
                conn.setAutoCommit(false);
            }
            // 返回执行结果
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /*
    创建一个 预编译sql语句创建器的回调接口，回调方法为返回一个预编译好的 PreparedStatement
     */
    private PreparedStatementCreator preparedStatementCreator(String sql, Object... args) {
        return conn -> {
            PreparedStatement ps = conn.prepareStatement(sql);
            bindArgs(ps, args);
            return ps;
        };
    }

    private void bindArgs(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }
}

class StringRowMapper implements RowMapper<String>{
    static StringRowMapper instance = new StringRowMapper();

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString(rowNum);
    }
}

class BooleanRowMapper implements RowMapper<Boolean> {
    static BooleanRowMapper instance = new BooleanRowMapper();

    @Override
    public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getBoolean(rowNum);
    }
}

class NumberRowMapper implements RowMapper<Number> {

    static NumberRowMapper instance = new NumberRowMapper();

    @Override
    public Number mapRow(ResultSet rs, int rowNum) throws SQLException {
        return (Number) rs.getObject(1);
    }
}
