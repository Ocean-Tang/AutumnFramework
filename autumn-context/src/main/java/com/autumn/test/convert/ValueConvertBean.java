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

    @Value("${convert.long}")
    private long longData;

    @Value("${convert.double}")
    private Double doubleData;

}
