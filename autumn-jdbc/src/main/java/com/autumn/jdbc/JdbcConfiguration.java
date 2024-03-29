package com.autumn.jdbc;

import com.autumn.annotation.Autowired;
import com.autumn.annotation.Bean;
import com.autumn.annotation.Configuration;
import com.autumn.annotation.Value;
import com.autumn.jdbc.tx.DataSourceTransactionManager;
import com.autumn.jdbc.tx.PlatformTransactionManager;
import com.autumn.jdbc.tx.TransactionalBeanPostProcessor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * @author huangcanjie
 */
@Configuration
public class JdbcConfiguration {

    @Bean(destroyMethod = "close")
    DataSource dataSource (
        @Value("${autumn.datasource.url}") String url,
        @Value("${autumn.datasource.username}") String username,
        @Value("${autumn.datasource.password}") String password,
        @Value("${autumn.datasource.driver-class-name:}") String driver,
        @Value("${autumn.datasource.maximum-pool-size:20}") int maximumPoolSize,
        @Value("${autumn.datasource.minimum-pool-size:1}") int minimumPoolSize,
        @Value("${autumn.datasource.connection-timeout:30000}") int connTimeout
    ) {
        HikariConfig config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (driver != null) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    TransactionalBeanPostProcessor transactionalBeanPostProcessor() {
        return new TransactionalBeanPostProcessor();
    }

    @Bean
    PlatformTransactionManager platformTransactionManager(@Autowired DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
