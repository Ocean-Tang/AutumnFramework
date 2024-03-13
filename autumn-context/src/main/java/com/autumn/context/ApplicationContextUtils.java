package com.autumn.context;

import java.util.Objects;

/**
 * @author huangcanjie
 */
public class ApplicationContextUtils {

    private static ApplicationContext applicationContext;

    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(getApplicationContext(), "ApplicationContext is not set.");
    }

    private static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    static void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }
}
