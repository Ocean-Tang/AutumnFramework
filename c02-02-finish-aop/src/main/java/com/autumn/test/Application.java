package com.autumn.test;

import com.autumn.annotation.Bean;
import com.autumn.annotation.ComponentScan;
import com.autumn.annotation.Configuration;
import com.autumn.aop.AroundProxyBeanPostProcessor;

/**
 * @author huangcanjie
 */
@ComponentScan
@Configuration
public class Application {

    @Bean
    AroundProxyBeanPostProcessor aroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}
