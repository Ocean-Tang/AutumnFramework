package com.autumn;

import com.autumn.annotation.Autowired;
import com.autumn.annotation.Bean;
import com.autumn.annotation.ComponentScan;
import com.autumn.annotation.Configuration;
import com.autumn.context.AnnotationConfigApplicationContext;
import com.autumn.io.PropertiesResolver;
import com.autumn.jdbc.JdbcTemplate;
import com.autumn.test.User;
import com.autumn.utils.YamlUtils;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

@Configuration
@ComponentScan
public class Main {

    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource){
        return new JdbcTemplate(dataSource);
    }

    public static final String CREATE_USER = "CREATE TABLE users (id INTEGER PRIMARY KEY auto_increment, name VARCHAR(255) NOT NULL, age INTEGER)";
    public static final String CREATE_ADDRESS = "CREATE TABLE addresses (id INTEGER PRIMARY KEY auto_increment, userId INTEGER NOT NULL, address VARCHAR(255) NOT NULL, zip INTEGER)";

    public static final String INSERT_USER = "INSERT INTO users (id, name, age) VALUES (?, ?, ?)";
    public static final String INSERT_ADDRESS = "INSERT INTO addresses (userId, address, zip) VALUES (?, ?, ?)";

    public static final String UPDATE_USER = "UPDATE users SET name = ?, age = ? WHERE id = ?";
    public static final String UPDATE_ADDRESS = "UPDATE addresses SET address = ?, zip = ? WHERE id = ?";

    public static final String DELETE_USER = "DELETE FROM users WHERE id = ?";
    public static final String DELETE_ADDRESS_BY_USERID = "DELETE FROM addresses WHERE userId = ?";

    public static final String SELECT_USER = "SELECT * FROM users WHERE id = ?";
    public static final String SELECT_USER_NAME = "SELECT name FROM users WHERE id = ?";
    public static final String SELECT_USER_AGE = "SELECT age FROM users WHERE id = ?";
    public static final String SELECT_ADDRESS_BY_USERID = "SELECT * FROM addresses WHERE userId = ?";

    public static final String DROP_USER = "drop table users";
    public static final String DROP_ADDRESS = "drop table addresses";


    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Main.class, getPropertiesResolver());
        JdbcTemplate jdbcTemplate = ctx.getBean(JdbcTemplate.class);
        jdbcTemplate.update(CREATE_USER);
        jdbcTemplate.update(CREATE_ADDRESS);

        int userId1 = jdbcTemplate.updateAndReturnGeneratedKey(INSERT_USER, 1788, "Bob", 12).intValue();
        int userId2 = jdbcTemplate.updateAndReturnGeneratedKey(INSERT_USER, 123129, "Alice", null).intValue();

        // query user:
        User bob = jdbcTemplate.queryForObject(SELECT_USER, User.class, userId1);
        User alice = jdbcTemplate.queryForObject(SELECT_USER, User.class, userId2);

        System.out.println(bob);
        System.out.println(alice);

        // query name by id
        String name1 = jdbcTemplate.queryForObject(SELECT_USER_NAME, String.class, userId1);
        String name2 = jdbcTemplate.queryForObject(SELECT_USER_NAME, String.class, userId2);
        System.out.println(name1);
        System.out.println(name2);

        // update user
        int n = jdbcTemplate.update(UPDATE_USER, "Bob Jones", 18, userId1);
        System.out.println(n);

        int delete = jdbcTemplate.update(DELETE_USER, userId2);
        System.out.println(delete);

        jdbcTemplate.update(DROP_USER);
        jdbcTemplate.update(DROP_ADDRESS);

        ctx.close();
    }

    private static PropertiesResolver getPropertiesResolver() {
        Properties properties = new Properties();
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("/application.yaml");
        properties.putAll(map);
        return new PropertiesResolver(properties);
    }
}