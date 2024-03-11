package com.autumn;

import com.autumn.context.AnnotationConfigApplicationContext;
import com.autumn.io.PropertiesResolver;
import com.autumn.test.ScanApplication;
import com.autumn.test.convert.ValueConvertBean;
import com.autumn.test.field.TestFieldBean;
import com.autumn.test.imported.LocalDateConfiguration;
import com.autumn.test.nested.OuterBean;
import com.autumn.test.primary.PersonBean;
import com.autumn.utils.YamlUtils;

import java.util.Map;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ScanApplication.class, getPropertiesResolver());
        // @Import
        System.out.println(ctx.findBeanDefinition(LocalDateConfiguration.class));
        System.out.println(ctx.findBeanDefinition("startLocalDate"));
        System.out.println(ctx.findBeanDefinition("startLocalDateTime"));

        // Nested
        System.out.println(ctx.findBeanDefinition(OuterBean.class));
        System.out.println(ctx.findBeanDefinition("nestedBean"));

        // 2个 PersonBean
        System.out.println(ctx.findBeanDefinition("studentBean"));
        System.out.println(ctx.findBeanDefinition("teacherBean"));
        System.out.println(ctx.findBeanDefinitions(PersonBean.class));
        System.out.println(ctx.findBeanDefinition(PersonBean.class));

        TestFieldBean bean = ctx.getBean(TestFieldBean.class);
        System.out.println(bean);
        System.out.println(bean.getOuterBean());
        System.out.println(bean.getPersonBean());

        ValueConvertBean valueConvertBean = ctx.getBean(ValueConvertBean.class);
        System.out.println(valueConvertBean);
    }

    private static PropertiesResolver getPropertiesResolver() {
        Properties properties = new Properties();
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("/application.yml");
        properties.putAll(map);
        return new PropertiesResolver(properties);
    }
}