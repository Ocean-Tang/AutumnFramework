package com.autumn.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author huangcanjie
 */
public abstract class AfterInvocationHandlerAdapter implements InvocationHandler {

    public abstract Object after(Object proxy, Object returnValue, Method method, Object[] args);

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(proxy, args);
        return after(proxy, result, method, args);
    }
}
