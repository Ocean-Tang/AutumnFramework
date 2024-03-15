package com.autumn.aop;

import lombok.Getter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author huangcanjie
 */
public abstract class AopInvocationHandler implements InvocationHandler {

    public void before(Object proxy, Method method, Object[] args) {
    }

    public Object after(Object proxy, Object returnValue, Method method, Object[] args) {
        return returnValue;
    }

    public Object around(JoinPoint joinPoint) throws Throwable {
        return joinPoint.process();
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        JoinPoint joinPoint = new JoinPoint(proxy, method, args);
        return this.around(joinPoint);
    }


    @Getter
    public class JoinPoint {
        Object target;
        Method method;
        Object[] args;

        public JoinPoint(Object proxy, Method method, Object[] args) {
            this.target = proxy;
            this.method = method;
            this.args = args;
        }

        public Object process() throws Throwable {
            AopInvocationHandler.this.before(target, method, args);
            Object returnValue = this.method.invoke(target, args);
            return AopInvocationHandler.this.after(target, returnValue, method, args);
        }
    }
}


