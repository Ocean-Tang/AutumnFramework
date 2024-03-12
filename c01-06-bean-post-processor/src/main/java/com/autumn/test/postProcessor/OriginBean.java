package com.autumn.test.postProcessor;

import com.autumn.annotation.Component;
import com.autumn.annotation.Value;
import lombok.Getter;
import lombok.ToString;

@Component
public class OriginBean {
    @Value("${app.title}")
    public String name;

    @Value("${app.version}")
    public String version;

    public String getName() {
        return name;
    }
}