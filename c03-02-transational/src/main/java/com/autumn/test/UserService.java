package com.autumn.test;

import com.autumn.annotation.Autowired;
import com.autumn.annotation.Component;
import com.autumn.annotation.Transactional;
import com.autumn.jdbc.JdbcTemplate;

import java.util.List;

/**
 * @author huangcanjie
 */
@Transactional
@Component
public class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    public static final String INSERT_USER = "INSERT INTO users (id, name, age) VALUES (?, ?, ?)";
    public static final String UPDATE_USER_ID = "UPDATE users set id=? where id = ?";
    public static final String QUERY_USER_BY_ID = "select * from users where id = ?";

    public User CreateUserBaseOtherUser(User user) {
        jdbcTemplate.update(UPDATE_USER_ID, user.id+1, user.id);
        // 故意抛异常
        System.out.println(user.id);
        User theUser = getUserById(user.id+1);
        System.out.println(theUser);
        User newUser = new User();
        newUser.setAge(theUser.theAge+2);
        newUser.id = theUser.id+2;
        newUser.name = theUser.name + "2";
        jdbcTemplate.update(INSERT_USER, newUser.id, newUser.name, newUser.theAge);
        return newUser;
    }

    public User createUser(User user) {
        jdbcTemplate.update(INSERT_USER, user.id, user.name, user.theAge);
        return user;
    }

    public User getUserById(int id) {
        return jdbcTemplate.queryForObject(QUERY_USER_BY_ID, User.class, id);
    }



}
