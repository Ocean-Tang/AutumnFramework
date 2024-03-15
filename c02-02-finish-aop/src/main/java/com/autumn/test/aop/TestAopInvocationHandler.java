package com.autumn.test.aop;

import com.autumn.annotation.Component;
import com.autumn.aop.AopInvocationHandler;

import java.lang.reflect.Method;

/**
 * @author huangcanjie
 */
@Component
public class TestAopInvocationHandler extends AopInvocationHandler {

    @Override
    public void before(Object proxy, Method method, Object[] args) {
        System.out.println(String.format("[Before] %s()", method.getName()));
    }

    @Override
    public Object after(Object proxy, Object returnValue, Method method, Object[] args) {
        Object value = returnValue instanceof String ? returnValue + "！！！！" : returnValue;
        System.out.println(String.format("[After] %s() 为返回内容添加内容：'！！！'，结果为： %s", method.getName(), value));
        return value;
    }

    @Override
    public Object around(JoinPoint joinPoint) throws Throwable {
        System.out.println(String.format("[Around - 1]执行方法%s前", joinPoint.getMethod().getName()));
        Object result = joinPoint.process();
        int length = result.toString().length()+2;
        StringBuilder top = new StringBuilder();
        StringBuilder bottom = new StringBuilder();
        bottom.append('\n');
        for (int i = 0; i < length; i++) {
            top.append('+');
            bottom.append('-');
        }
        top.append('\n');
        bottom.append('\n');
        String newResult = top.toString() + result + bottom;
        System.out.println(String.format("[Around - 2]执行方法%s后，为返回结果添加包裹", joinPoint.getMethod().getName(), result, newResult));
        return newResult;
    }
}
