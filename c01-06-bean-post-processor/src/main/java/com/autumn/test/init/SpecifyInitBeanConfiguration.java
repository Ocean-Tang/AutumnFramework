package com.autumn.test.init;

import com.autumn.annotation.Bean;
import com.autumn.annotation.Configuration;
import com.autumn.annotation.Value;

/**
 * @author huangcanjie
 */
@Configuration
public class SpecifyInitBeanConfiguration {

    @Bean(initMethod = "init")
    public SpecifyInitBean specifyInitBean(@Value("${app.title}") String title, @Value("${app.version}") String version) {
        return new SpecifyInitBean(title, version);
    }
}
