package com.autumn.test.postProcessor;

import com.autumn.annotation.Component;
import com.autumn.annotation.Value;
import lombok.ToString;

@Component
@ToString
public class OriginBean {
    @Value("${app.title}")
    public String name;

    @Value("${app.version}")
    public String version;

    public String getName() {
        return name;
    }
}