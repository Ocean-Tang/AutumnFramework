package com.autumn.context;

import com.autumn.exception.NoSuchBeanDefinitionException;

import java.util.List;

/**
 * 用户级别接口
 */
public interface ApplicationContext extends AutoCloseable{

    /**
     * 是否存在指定名称的Bean
     * @param name  bean的名称
     * @return      存在返回true
     */
    boolean containsBean(String name);

    /**
     * 获取指定名称的唯一Bean，未找到抛出NoSuchBeanDefinitionException
     */
    <T> T getBean(String name) ;

    /**
     * 根据name返回唯一Bean，未找到抛出NoSuchBeanDefinitionException，找到但type不符抛出BeanNotOfRequiredTypeException
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * 根据type返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * 根据type返回一组Bean，未找到返回空List
     */
    <T> List<T> getBeans(Class<T> requiredType);

    /**
     * 关闭并执行所有bean的destroy方法
     */
    void close();
}
