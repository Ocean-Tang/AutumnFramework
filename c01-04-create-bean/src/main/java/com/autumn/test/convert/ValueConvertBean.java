package com.autumn.test.convert;

import com.autumn.annotation.Component;
import com.autumn.annotation.Value;
import lombok.ToString;

/**
 * @author huangcanjie
 */
@Component
@ToString
public class ValueConvertBean {

    @Value("${convert.intData}")
    private int intData;

    @Value("${convert.double}")
    private Double doubleDate;

}
