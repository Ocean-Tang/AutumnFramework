package com.autumn.test.postProcessor;

import com.autumn.annotation.Component;
import com.autumn.annotation.Order;
import com.autumn.context.BeanPostProcessor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Order(100)
@Component
public class FirstProxyBeanPostProcessor implements BeanPostProcessor {
    // 保存原始Bean:
    Map<String, Object> originBeans = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (OriginBean.class.isAssignableFrom(bean.getClass())) {
            // 检测到OriginBean,创建FirstProxyBean:
            FirstProxyBean proxy = new FirstProxyBean((OriginBean) bean);
            // 保存原始Bean:
            originBeans.put(beanName, bean);
            // 返回Proxy:
            return proxy;
        }
        return bean;
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = originBeans.get(beanName);
        if (origin != null) {
            // 存在原始Bean时,返回原始Bean:
            return origin;
        }
        return bean;
    }

}

// 代理Bean:
class FirstProxyBean extends OriginBean {
    final OriginBean target;

    public FirstProxyBean(OriginBean target) {
        this.target = target;
    }
}