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

    @SuppressWarnings("unchecked")
    public <T> T createProxy(T bean, InvocationHandler handler) {
        Class<?> targetClass = bean.getClass();
        logger.debug("create proxy for bean {} @{}", targetClass.getName(), Integer.toHexString(bean.hashCode()));
        Class<?> proxyClass = this.byteBuddy
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                .method(ElementMatchers.isPublic())
                .intercept(InvocationHandlerAdapter.of(
                        (proxy, method, args) -> handler.invoke(bean, method, args)
                ))
                .make()
                .load(targetClass.getClassLoader())
                .getLoaded();

        Object proxy;

        try {
            proxy = proxyClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T)proxy;
    }

}
