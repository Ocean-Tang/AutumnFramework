package com.autumn;

import com.autumn.context.AnnotationConfigApplicationContext;
import com.autumn.io.PropertiesResolver;
import com.autumn.test.Application;
import com.autumn.test.around.OriginBean;
import com.autumn.utils.YamlUtils;

import java.util.Map;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Application.class, getPropertiesResolver());

        OriginBean bean = ctx.getBean(OriginBean.class);

        System.out.println(bean.hello());
        System.out.println(bean.morning());


        ctx.close();
    }

    private static PropertiesResolver getPropertiesResolver() {
        Properties properties = new Properties();
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("/application.yml");
        properties.putAll(map);
        return new PropertiesResolver(properties);
    }
}