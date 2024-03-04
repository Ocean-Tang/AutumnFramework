package com.autumn;

import com.autumn.io.PropertiesResolver;
import com.autumn.utils.YamlUtils;

import java.time.LocalTime;
import java.util.Map;
import java.util.Properties;

/**
 * @author huangcanjie
 */
public class Main {

    public static void main(String[] args) {
        Properties properties = new Properties();
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("/application.yml");
        properties.putAll(map);
        PropertiesResolver propertiesResolver = new PropertiesResolver(properties);

        String url = propertiesResolver.getProperty("summer.datasource.url");
        String title = propertiesResolver.getProperty("${app.title:${HOME}}");
        LocalTime localTime = propertiesResolver.getProperty("convert.localtime", LocalTime.class);
        System.out.println(url);
        System.out.println(title);
        System.out.println(localTime);
    }
}
