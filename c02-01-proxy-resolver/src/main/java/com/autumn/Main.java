package com.autumn;

import com.autumn.aop.ProxyResolver;
import com.autumn.test.OriginBean;
import com.autumn.test.PoliteInvocationHandler;

public class Main {
    public static void main(String[] args) {
        OriginBean originBean = new OriginBean();
        originBean.name = "Bob";

        ProxyResolver proxyResolver = new ProxyResolver();
        OriginBean proxy = proxyResolver.createProxy(originBean, new PoliteInvocationHandler());

        System.out.println(originBean.getClass().getName());

        System.out.println(proxy.getClass().getName());

        System.out.println(originBean.getClass() == proxy.getClass());

        System.out.println(proxy.name);

        System.out.println(proxy.hello());
        System.out.println(proxy.morning());
    }
}