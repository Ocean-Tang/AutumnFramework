package com.autumn.test.around;

import com.autumn.annotation.Autowired;
import com.autumn.annotation.Component;
import com.autumn.annotation.Order;

@Order(0)
@Component
public class OtherBean {

    public OriginBean origin;

    public OtherBean(@Autowired OriginBean origin) {
        this.origin = origin;
    }
}