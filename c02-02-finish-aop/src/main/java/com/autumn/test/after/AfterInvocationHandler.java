package com.autumn.test.after;

import com.autumn.annotation.Component;
import com.autumn.aop.AfterInvocationHandlerAdapter;

import java.lang.reflect.Method;

/**
 * @author huangcanjie
 */
@Component
public class AfterInvocationHandler extends AfterInvocationHandlerAdapter {

    @Override
    public Object after(Object proxy, Object returnValue, Method method, Object[] args) {
        if (returnValue instanceof String) {
            return "我被替换了！";
        }
        return returnValue;
    }
}
