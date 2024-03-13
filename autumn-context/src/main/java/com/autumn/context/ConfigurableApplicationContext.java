package com.autumn.context;

import com.sun.istack.internal.Nullable;

import java.util.List;

/**
 * 框架级别接口
 */
public interface ConfigurableApplicationContext extends ApplicationContext{

    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(String name);

    @Nullable
    BeanDefinition findBeanDefinition(String name, Class<?> requiredType);

    Object createBeanAsEarlySingleton(BeanDefinition def);

}
