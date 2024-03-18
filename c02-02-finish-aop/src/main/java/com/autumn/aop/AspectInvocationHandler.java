package com.autumn.aop;

import lombok.Getter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author huangcanjie
 */
public abstract class AspectInvocationHandler implements InvocationHandler {

    public void before(Object proxy, Method method, Object[] args) {
    }

    public Object after(Object proxy, Object returnValue, Method method, Object[] args) {
        return returnValue;
    }

    public Object around(JoinPoint joinPoint) throws Throwable {
        return joinPoint.process();
    }

    public void afterReturn (Object proxy, Method method, Object[] args) {
    }

    public void afterThrowing (Object proxy, Method method, Object[] args, Throwable e) {
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        JoinPoint joinPoint = new JoinPoint(proxy, method, args);
        Object result = this.around(joinPoint);
        this.afterReturn(proxy, method, args);
        return result;
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

        /**
         * 执行被代理方法
         */
        public Object process() {
            AspectInvocationHandler.this.before(target, method, args);
            Object returnValue = null;
            try {
                returnValue = this.method.invoke(target, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                AspectInvocationHandler.this.afterThrowing(target, method, args, e);
            }
            return AspectInvocationHandler.this.after(target, returnValue, method, args);
        }
    }
}


