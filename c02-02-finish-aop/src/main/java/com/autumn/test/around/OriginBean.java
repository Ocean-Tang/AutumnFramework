package com.autumn.test.around;

import com.autumn.annotation.Around;
import com.autumn.annotation.Component;
import com.autumn.annotation.Value;

/**
 * @author huangcanjie
 */
@Component
@Around("aroundInvocationHandler")
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
        return "Morning, " + name + ".";
    }
}
