package com.autumn.test.init;

/**
 * @author huangcanjie
 */
public class SpecifyInitBean {

    String appTitle;
    String appVersion;

    public SpecifyInitBean(String appTitle, String appVersion) {
        this.appTitle = appTitle;
        this.appVersion = appVersion;
    }

    public void init() {
        System.out.println(String.format("使用 @Bean 注解标注的初始化方法执行成功"));
    }
}
