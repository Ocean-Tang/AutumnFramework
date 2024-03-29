package com.autumn.context;

public interface BeanPostProcessor {

    /**
     * 在Bean 实例化后调用
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 在 Bean 执行初始化方法之后调用
     */
    default Object postProcessAfterInitialization (Object bean, String beanName) {
        return bean;
    }

    /**
     * 在设置Bean 的属性之前调用
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessOnSetProperty(Object bean, String beanName) {
        return bean;
    }
}
