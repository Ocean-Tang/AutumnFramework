package com.autumn.context;

import com.autumn.annotation.Bean;
import com.autumn.annotation.Component;
import com.autumn.annotation.ComponentScan;
import com.autumn.annotation.Configuration;
import com.autumn.annotation.Import;
import com.autumn.annotation.Order;
import com.autumn.annotation.Primary;
import com.autumn.exception.BeanDefinitionException;
import com.autumn.exception.BeanNotOfRequiredTypeException;
import com.autumn.exception.NoUniqueBeanDefinitionException;
import com.autumn.io.PropertiesResolver;
import com.autumn.io.ResourceResolver;
import com.autumn.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author huangcanjie
 */
public class AnnotationConfigApplicationContext {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final PropertiesResolver propertiesResolver;
    protected final Map<String, BeanDefinition> beans;

    // 记录正在创建的 bean，解决循环依赖
    private Set<String> creatingBeanNames;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertiesResolver propertiesResolver) {
        this.propertiesResolver = propertiesResolver;

        // 扫描获取所有 Bean 的 CLASS 类型, configClass 标注应该标注了 @ComponentScan
        final Set<String> beanClassNames = scanForClassNames(configClass);

        // 创建 Bean 的定义
        this.beans = createBeanDefinition(beanClassNames);

        // 创建 Bean  循环依赖检测器
        this.creatingBeanNames = new HashSet<>();

        // 创建 @Configuration 类型的 Bean
        List<String> collect = this.beans.values().stream()
                .filter(this::isConfigurationDefinition)
                .sorted()
                .map(def -> {
                    createBeanAsEarlySingleton(def);
                    return def.getName();
                })
                .collect(Collectors.toList());
    }

    private Set<String> scanForClassNames(Class<?> configClass) {
        // 扫描配置了 @ComponentScan 中的包名，递归查找当前类的注解中，是否有 @ComponentScan
        ComponentScan componentScan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        // 获取包名，如果没有配置，获取配置类所在包
        final String[] scanPackages = componentScan == null || componentScan.value().length == 0 ? new String[]{configClass.getPackage().getName()} : componentScan.value();
        log.info("component scan in packages: {}", Arrays.toString(scanPackages));

        // 使用 ResourceResolver 收集指定包下的 Class 资源，得到他们的类名
        Set<String> classNameSet = new HashSet<>();
        for (String pkg : scanPackages) {
            log.debug("scan package: {}", pkg);
            ResourceResolver resourceResolver = new ResourceResolver(pkg);
            List<String> classList = resourceResolver.scan(r -> {
                String name = r.getName();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace('/', '.').replace('\\', '.');
                }
                return null;
            });

            if (log.isDebugEnabled()) {
                classList.forEach(c -> log.debug("class found by component scan: {}", c));
            }

            classNameSet.addAll(classList);
        }

        // 查找 @Import 中引入的类
        Import importConfig = ClassUtils.findAnnotation(configClass, Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()) {
                String importClassName = importConfigClass.getName();
                if (classNameSet.contains(importClassName)) {
                    log.warn("ignore import: {} for it is already been scaned.", importClassName);
                } else {
                    log.debug("class found by import: {}.", importClassName);
                    classNameSet.add(importClassName);
                }
            }

        }
        return classNameSet;
    }

    Map<String, BeanDefinition> createBeanDefinition(Set<String> beanClassNames) {
        Map<String, BeanDefinition> defs = new HashMap<>();

        for (String className : beanClassNames) {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            // 如果为注解、枚举、接口、或者新版本的 record，则不创建 BeanDefinition
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface()) {
                continue;
            }
            /* 处理 @Component */
            // 当前类是否标注了 @Component，标注了则开始创建 BeanDefinition
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                log.debug("found component: {}", clazz.getName());

                // 如果是抽象类、私有类，则报错
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException(String.format("@Component class %s must not be abstract", clazz.getName()));
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException(String.format("@Component class %s must not be private", clazz.getName()));
                }

                // 获取 bean 的名称，递归获取其中的 @Component，拿到其定义的 bean 名称，如果没有定义，则将类名转化为 bean 名称
                String beanName = ClassUtils.getBeanName(clazz);
                BeanDefinition def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        null, null,
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                addBeanDefinitions(defs, def);
                log.debug("defind bean: {}", def);
                /* 处理 @Component */

                // 如果是配置类，获取其中被 @Bean 标注的方法创建的 实例
                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    scanFactoryMethods(beanName, clazz, defs);
                }
            }
        }
        return defs;
    }

    private void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        for (Method method : clazz.getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                // 如果是抽象类、final类、私有类，则报错
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException(String.format("@Bean method %s#%s must not be abstract", clazz.getName(), method.getName()));
                }
                if (Modifier.isFinal(mod)) {
                    throw new BeanDefinitionException(String.format("@Bean method %s#%s must not be final", clazz.getName(), method.getName()));
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException(String.format("@Bean method %s#%s must not be private", clazz.getName(), method.getName()));
                }

                // 如果方法返回类型是基本类型或者是 void，则报错
                Class<?> beanClass = method.getReturnType();
                if (beanClass.isPrimitive()) {
                    throw new BeanDefinitionException(String.format("@Bean method %s#%s must not return primitive type.", clazz.getName(), method.getName()));
                }
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new BeanDefinitionException(String.format("@Bean method %s#%s must not return void.", clazz.getName(), method.getName()));
                }

                BeanDefinition beanDefinition = new BeanDefinition(ClassUtils.getBeanName(method), beanClass, factoryBeanName,
                        method,
                        getOrder(method), method.isAnnotationPresent(Primary.class),
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        null,
                        null);
                addBeanDefinitions(defs, beanDefinition);
                log.debug("define bean: {}", beanDefinition);
            }
        }
    }

    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 0) {
            constructors = clazz.getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new BeanDefinitionException(String.format("More than one constructor found in class %s.", clazz.getName()));
            }
        }
        if (constructors.length != 1) {
            throw new BeanDefinitionException(String.format("More than one public constructor found in class %s.", clazz.getName()));
        }
        return constructors[0];
    }

    void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name:" + def.getName());
        }
    }

    boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }

    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition beanDefinition = findBeanDefinition(name);
        if (beanDefinition == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(beanDefinition.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.",
                    requiredType.getName(),
                    name,
                    beanDefinition.getBeanClass().getName()));
        }
        return beanDefinition;
    }

    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream().filter(b -> type.isAssignableFrom(b.getBeanClass()))
                .sorted().collect(Collectors.toList());
    }

    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> beanDefinitions = findBeanDefinitions(type);
        if (beanDefinitions.isEmpty()) {
            return null;
        }
        if (beanDefinitions.size() == 1) {
            return beanDefinitions.get(0);
        }

        List<BeanDefinition> primaryDefs = beanDefinitions.stream().filter(b -> b.isPrimary()).collect(Collectors.toList());
        if (primaryDefs.size() == 1) {
            return primaryDefs.get(0);
        }
        if (primaryDefs.isEmpty()){
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }

}
