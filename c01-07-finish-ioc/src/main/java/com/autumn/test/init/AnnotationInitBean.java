package com.autumn.test.init;

import com.autumn.annotation.Autowired;
import com.autumn.annotation.Component;
import com.autumn.test.primary.StudentBean;

import javax.annotation.PostConstruct;

/**
 * @author huangcanjie
 */
@Component
public class AnnotationInitBean {

    @Autowired
    private StudentBean studentBean;

    @PostConstruct
    public void init() {
        System.out.println(String.format("%s 初始化成功： %s", this.getClass().getName(), this));
    }
}
