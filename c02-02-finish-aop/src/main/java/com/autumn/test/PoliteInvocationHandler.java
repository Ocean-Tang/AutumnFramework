package com.autumn.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author huangcanjie
 */
public class PoliteInvocationHandler implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getAnnotation(Polite.class) != null) {
            String result = (String) method.invoke(proxy, args);
            if (result.endsWith(".")) {
                result = result.substring(0, result.length()-1) + "!";
            }
            return result;
        }
        return method.invoke(proxy,args);
    }
}
