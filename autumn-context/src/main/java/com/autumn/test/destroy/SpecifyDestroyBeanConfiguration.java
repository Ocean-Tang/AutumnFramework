package com.autumn.test.destroy;

import com.autumn.annotation.Bean;
import com.autumn.annotation.Configuration;

/**
 * @author huangcanjie
 */
@Configuration
public class SpecifyDestroyBeanConfiguration {

    @Bean
    public SpecifyDestroyBean specifyDestroyBean() {
        return new SpecifyDestroyBean();
    }
}
