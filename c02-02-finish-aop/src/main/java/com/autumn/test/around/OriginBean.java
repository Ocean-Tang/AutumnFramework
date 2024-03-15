package com.autumn.test.around;

import com.autumn.annotation.Around;
import com.autumn.annotation.Aspect;
import com.autumn.annotation.Component;
import com.autumn.annotation.Value;

/**
 * @author huangcanjie
 */
@Component
@Aspect("testAopInvocationHandler")
public class OriginBean {

    @Value("${app.title}")
    public String name;

    public OriginBean() {
    }

    @Polite
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        int i = 1/0;
        return "Morning, " + name + ".";
    }
}
