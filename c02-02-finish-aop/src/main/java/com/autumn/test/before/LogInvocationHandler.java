package com.autumn.test.before;

import com.autumn.annotation.Component;
import com.autumn.aop.BeforeInvocationHandlerAdapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author huangcanjie
 */
@Component
public class LogInvocationHandler extends BeforeInvocationHandlerAdapter {

    @Override
    public void before(Object proxy, Method method, Object[] args) {
        System.out.println("[Before] " + method.getName() + "()");
    }
}
