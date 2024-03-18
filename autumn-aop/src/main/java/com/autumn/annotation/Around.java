package com.autumn.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited // 如果A类标注了当前注解，则 B类（是A的子类）也会继承该注解
@Documented
public @interface Around {

    /**
     * Invocation handler bean name.
     */
    String value();
}
