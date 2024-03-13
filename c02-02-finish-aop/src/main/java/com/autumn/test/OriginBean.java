package com.autumn.test;

/**
 * @author huangcanjie
 */
public class OriginBean {

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
