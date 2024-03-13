package com.autumn.test.postProcessor;

import com.autumn.annotation.Autowired;
import com.autumn.annotation.Component;

@Component
public class InjectProxyOnConstructorBean {
    @Autowired
    public OriginBean injected;

    /*public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }*/
}