package com.autumn.aop;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author huangcanjie
 */
public class ProxyResolver {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final ByteBuddy byteBuddy = new ByteBuddy();

    /**
     * 创建代理实例
     * @param bean          原始Bean
     * @param handler       拦截器
     * @return              代理类实例
     * @param <T>           原始Bean的类型
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T bean, InvocationHandler handler) {
        Class<?> targetClass = bean.getClass();
        logger.debug("create proxy for bean {} @{}", targetClass.getName(), Integer.toHexString(bean.hashCode()));
        // 创建代理类
        Class<?> proxyClass = this.byteBuddy
                //  代理类是 targetClass 的子类，
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                // 拦截所有 public 方法
                .method(ElementMatchers.isPublic())
                .intercept(InvocationHandlerAdapter.of(
                        // 调用传进来的拦截器的方法，代理原始 bean。 由 ByteBuddy 负责方法、方法参数的传递
                        (proxy, method, args) -> handler.invoke(bean, method, args)
                ))
                // 生成字节码
                .make()
                // 加载字节码
                .load(targetClass.getClassLoader())
                .getLoaded();

        // 创建代理类实例
        Object proxy;

        try {
            proxy = proxyClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T)proxy;
    }

}
