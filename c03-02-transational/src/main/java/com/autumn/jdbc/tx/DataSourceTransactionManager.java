package com.autumn.jdbc.tx;

import com.autumn.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author huangcanjie
 */
public class DataSourceTransactionManager implements PlatformTransactionManager, InvocationHandler {

    static final ThreadLocal<TransactionStatus> transactionStatus = new ThreadLocal<>();

    final Logger logger = LoggerFactory.getLogger(getClass());

    final DataSource dataSource;

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 获取当前的数据库连接，如果存在，直接执行方法
        TransactionStatus status = transactionStatus.get();
        if (status == null) {
            // 当前不存在数据库事务连接，获取新的连接
            try (Connection connection = dataSource.getConnection()) {
                boolean autoCommit = connection.getAutoCommit();
                // 关闭自动提交
                if (autoCommit) {
                    connection.setAutoCommit(false);
                }
                try {
                    // 保存数据库连接，方便下一个方法加入事务
                    transactionStatus.set(new TransactionStatus(connection));
                    // 执行方法，等待调用完成（可能会调用其它事务方法，其他事务方法会直接执行 else 分支)，提交事务
                    Object result = method.invoke(proxy, args);
                    connection.commit();
                    return result;
                } catch (InvocationTargetException e) {
                    logger.warn("will rollback transaction for caused exception: {}", e.getCause() == null ? "null" : e.getCause().getClass().getName());
                    TransactionException exception = new TransactionException(e.getCause());
                    // 发生异常，回滚事务
                    try {
                        connection.rollback();
                    } catch (SQLException ex) {
                        exception.addSuppressed(ex);
                    }
                    throw exception;
                } finally {
                    // 移除当前事务连接，并设置自动提交
                    transactionStatus.remove();
                    if (autoCommit) {
                        connection.setAutoCommit(true);
                    }
                }
            }
        } else {
            return method.invoke(proxy, args);
        }
    }
}
