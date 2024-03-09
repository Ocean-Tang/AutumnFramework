package com.autumn.io;

import com.sun.istack.internal.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

/**
 * @author huangcanjie
 */
public class PropertiesResolver {

    Logger log = LoggerFactory.getLogger(getClass());

    Map<String, String> properties = new HashMap<>();
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertiesResolver(Properties properties) {
        // 存入系统变量
        this.properties.putAll(System.getenv());
        Set<String> names = properties.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, properties.getProperty(name));
        }

        if (log.isDebugEnabled()) {
            List<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                log.debug("PropertyResolver: {} = {}", key, this.properties.get(key));
            }
        }

        converters.put(String.class, s -> s);
        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::valueOf);

        converters.put(byte.class, Byte::parseByte);
        converters.put(Byte.class, Byte::valueOf);

        converters.put(short.class, Short::parseShort);
        converters.put(Short.class, Short::valueOf);

        converters.put(int.class, Integer::parseInt);
        converters.put(Integer.class, Integer::valueOf);

        converters.put(long.class, Long::parseLong);
        converters.put(Long.class, Long::valueOf);

        converters.put(float.class, Float::parseFloat);
        converters.put(Float.class, Float::valueOf);

        converters.put(double.class, Double::parseDouble);
        converters.put(Double.class, Double::valueOf);

        converters.put(LocalDate.class, LocalDate::parse);
        converters.put(LocalTime.class, LocalTime::parse);
        converters.put(LocalDateTime.class, LocalDateTime::parse);
        converters.put(ZonedDateTime.class, ZonedDateTime::parse);
        converters.put(Duration.class, Duration::parse);
        converters.put(ZoneId.class, ZoneId::of);
    }

    public boolean containProperty (String key) {
        return this.properties.containsKey(key);
    }

    @Nullable
    public String getProperty (String key) {
        // key 有可能为 ${abc.xyz:${def:DefaultValue}}
        // 解析 key，并转换为 PropertyExpr 对象
        PropertyExpr keyExpr = parsePropertyExpr(key);
        if (keyExpr != null) {
            if (keyExpr.getDefaultValue() != null) {
                // 如果有默认值，继续解析，可能得到嵌套的下一层
                return getProperty(keyExpr.getKey(), keyExpr.getDefaultValue());
            } else {
                return getRequiredProperty(keyExpr.getKey());
            }
        }
        // 如果不是表达式 key，则直接从保存的键值对中获取 value 并解析
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return value;
    }

    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(targetType, value);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }

    @SuppressWarnings("unchecked")
    <T> T convert(Class<T> clazz, String value) {
        Function<String, Object> function = this.converters.get(clazz);
        if (function == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T)function.apply(value);
    }

    String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    String parseValue(String value) {
        // value 有可能是一个 表达式 key
        PropertyExpr expr = parsePropertyExpr(value);
        if (expr == null) {
            return value;
        }
        if (expr.getDefaultValue() != null) {
            return getProperty(expr.getKey(), expr.getDefaultValue());
        } else {
            return getRequiredProperty(expr.getKey());
        }
    }

    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    PropertyExpr parsePropertyExpr(String key) {
        // 键值对可能为 ${abc.xyz:defaultValue}
        if (key.startsWith("${") && key.endsWith("}")) {
            int n = key.indexOf(':');
            if (n == -1) {
                // 没有默认值
                String k = notEmpty(key.substring(2, key.length()-1));
                return new PropertyExpr(k, null);
            } else {
                // 有默认值
                String k = notEmpty(key.substring(2, n));
                return new PropertyExpr(k, key.substring(n+1, key.length()-1));
            }
        }
        return null;
    }

    String notEmpty(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }

    @Getter
    @AllArgsConstructor
    class PropertyExpr {
        private String key;
        private String defaultValue;
    }
}
