package com.autumn.test.destroy;

import com.autumn.annotation.Component;

import javax.annotation.PreDestroy;

/**
 * @author huangcanjie
 */
@Component
public class AnnotationDestroyBean {

    @PreDestroy
    public void destroy() {
        System.out.println("调用AnnotationDestroyBean的销毁方法");
    }
}
