package com.autumn.test.nested;

import com.autumn.annotation.Component;

/**
 * @author huangcanjie
 */
@Component
public class OuterBean {

    @Component
    public static class NestedBean {

    }
}
