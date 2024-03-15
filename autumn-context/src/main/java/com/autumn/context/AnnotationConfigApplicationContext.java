package com.autumn.context;

import com.autumn.annotation.Autowired;
import com.autumn.annotation.Bean;
import com.autumn.annotation.Component;
import com.autumn.annotation.ComponentScan;
import com.autumn.annotation.Configuration;
import com.autumn.annotation.Import;
import com.autumn.annotation.Order;
import com.autumn.annotation.Primary;
import com.autumn.annotation.Value;
import com.autumn.exception.BeanCreationException;
import com.autumn.exception.BeanDefinitionException;
import com.autumn.exception.BeanNotOfRequiredTypeException;
import com.autumn.exception.NoSuchBeanDefinitionException;
import com.autumn.exception.NoUniqueBeanDefinitionException;
import com.autumn.exception.UnsatisfiedDependencyException;
import com.autumn.io.PropertiesResolver;
import com.autumn.io.ResourceResolver;
import com.autumn.utils.ClassUtils;
import com.sun.istack.internal.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 改造获取 BeanPostProcessor 执行后，获取原始Bean注入属性的流程
 * @author huangcanjie
 */
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext{

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final PropertiesResolver propertiesResolver;
    protected final Map<String, BeanDefinition> beans;

    // 记录正在创建的 bean，解决循环依赖
    private Set<String> creatingBeanNames;
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    // 保存原始 Bean
    private Map<String, Object> originBeanMap = new HashMap<>();

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertiesResolver propertiesResolver) {
        ApplicationContextUtils.setApplicationContext(this);

        this.propertiesResolver = propertiesResolver;

        // 扫描获取所有 Bean 的 CLASS 类型, configClass 标注应该标注了 @ComponentScan
        final Set<String> beanClassNames = scanForClassNames(configClass);

        // 创建 Bean 的定义
        this.beans = createBeanDefinitions(beanClassNames);

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

        // 创建 BeanPostProcessor 类型的 Bean
        List<BeanPostProcessor> processors = this.beans.values().stream()
                .filter(this::isBeanPostProcessorDefinition)
                .sorted()
                .map(def -> ((BeanPostProcessor) createBeanAsEarlySingleton(def)))
                .collect(Collectors.toList());
        this.beanPostProcessors.addAll(processors);

        // 创建其他普通 Bean
        createNormalBeans();

        // 通过字段和 set 方法注入
        this.beans.values().forEach(this::injectBean);

        // 调用 init 方法
        this.beans.values().forEach(this::initBean);

        if (log.isDebugEnabled()) {
            this.beans.values().stream().sorted().forEach(def -> {
                log.debug("bean initialized: {}", def);
            });
        }
    }

    boolean isBeanPostProcessorDefinition(BeanDefinition def) {
        return BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
    }

    /**
     * 调用 init 方法
     * @param def       BeanDefinition
     */
    void initBean(BeanDefinition def) {
        final Object proxiedInstance = getProxiedInstance(def);

        callMethod(proxiedInstance, def.getInitMethod(), def.getInitMethodName());

        beanPostProcessors.forEach(processor -> {
            Object processed = processor.postProcessAfterInitialization(def.getInstance(), def.getName());
            if (processed != def.getInstance()) {
                log.debug("BeanPostProcessor {} return different bean from {} to {}.", processor.getClass().getSimpleName(),
                        def.getInstance().getClass().getName(), processed.getClass().getName());
                this.originBeanMap.putIfAbsent(def.getName(), def.getInstance());
                def.setInstance(processed);
            }
        });
    }

    private void callMethod(Object instance, Method method, String methodName) {
        if (method != null) {
            try {
                method.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new BeanCreationException(e);
            }
        } else if (methodName != null){
            Method namedMethod = ClassUtils.getNamedMethod(instance.getClass(), methodName);
            namedMethod.setAccessible(true);
            try {
                namedMethod.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new BeanCreationException(e);
            }
        }
    }

    /**
     * 注入依赖，但是不调用 init 方法
     * @param def       BeanDefinition
     */
    void injectBean(BeanDefinition def) {
        final Object beanInstance = getProxiedInstance(def);
        try {
            injectProperties(def, def.getBeanClass(), beanInstance);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    Object getProxiedInstance(BeanDefinition def) {
        return this.originBeanMap.getOrDefault(def.getName(), def.getInstance());
        /*Object beanInstance = def.getInstance();
        // 如果Proxy改变了原始Bean，又希望注入到原始Bean，则由BeanPostProcessor指定原始Bean:
        List<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(this.beanPostProcessors);
        Collections.reverse(reversedBeanPostProcessors);
        for (BeanPostProcessor processor : reversedBeanPostProcessors) {
            Object restoredInstance = processor.postProcessOnSetProperty(beanInstance, def.getName());
            if (restoredInstance != beanInstance) {
                log.debug("BeanPostProcessor {} specified injection from {} to {}.", processor.getClass().getSimpleName(),
                        beanInstance.getClass().getSimpleName(), restoredInstance.getClass().getSimpleName());
                beanInstance = restoredInstance;
            }
        }
        return beanInstance;*/
    }


    void createNormalBeans() {
        // 过滤出还没有创建实例的 Bean
        List<BeanDefinition> defs = this.beans.values().stream()
                .filter(def -> def.getInstance() == null).sorted().collect(Collectors.toList());

        defs.forEach(def -> {
            // 可能在其他 Bean 构造方法注入时已经被创建，因此再判断一次
            if (def.getInstance() == null) {
                createBeanAsEarlySingleton(def);
            }
        });
    }

    /**
     * 创建一个 Bean，不进行字段和方法级别的注入。
     * 如果创建的 Bean 不是 Configuration，则在构造方法中注入的依赖 Bean 会自动创建
     *
     * @param def BeanDefinition
     */
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        log.debug("Try create bean '{}' as early singleton: {}", def.getName(), def.getBeanClass().getName());

        // 发现循环依赖
        if (!this.creatingBeanNames.add(def.getName())) {
            throw new UnsatisfiedDependencyException(String.format("Circular dependency detected when create bean '%s'", def.getName()));
        }

        // 创建方式：构造方法或者工厂方法
        Executable createFunction = null;
        if (def.getFactoryMethod() != null) {
            createFunction = def.getFactoryMethod();
        } else {
            createFunction = def.getConstructor();
        }

        // 获取方法的参数和参数上的注解
        final Parameter[] parameters = createFunction.getParameters();
        final Annotation[][] paramsAnnos = createFunction.getParameterAnnotations();
        // 传递的参数值
        Object[] args = new Object[parameters.length];

        // 创建参数上所需要的 Bean
        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            final Annotation[] annotations = paramsAnnos[i];
            final Value value = ClassUtils.getAnnotation(annotations, Value.class);
            final Autowired autowired = ClassUtils.getAnnotation(annotations, Autowired.class);

            // 如果是 @Configuration 标注的 Bean，则为工厂，不允许使用 @AutoWired 创建。
            final boolean isConfiguration = isConfigurationDefinition(def);
            if (isConfiguration && autowired != null) {
                throw new BeanCreationException(String.format("Cannot specify @Autowired when create @Configuration bean '%s': '%s'.", def.getName(), def.getBeanClass().getName()));
            }

            // 参数需要 @Value 或者 @Autowired 两者之一
            if (value != null && autowired != null) {
                throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when create bean '%s': '%s'.", def.getName(), def.getBeanClass().getName()));
            }
            if (value == null && autowired == null) {
                throw new BeanCreationException(String.format("Must specify @Autowired or @Value when create bean '%s': '%s'.", def.getName(), def.getBeanClass().getName()));
            }

            final Class<?> type = parameter.getType();
            if (value != null) {
                args[i] = this.propertiesResolver.getRequiredProperty(value.value(), type);
            } else {
                String name = autowired.name();
                boolean required = autowired.value();

                BeanDefinition beanDefinition = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                // 检查这个参数是必须的
                if (required && beanDefinition == null) {
                    throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': '%s'.", type.getName(), def.getName(), def.getBeanClass().getName()));
                }

                if (beanDefinition != null) {
                    Object autowiredBeanInstance = beanDefinition.getInstance();
                    if (autowiredBeanInstance == null && !isConfiguration) {
                        autowiredBeanInstance = createBeanAsEarlySingleton(beanDefinition);
                    }
                    args[i] = autowiredBeanInstance;
                } else {
                    args[i] = null;
                }
            }
        }

        Object instance = null;
        if (def.getConstructor() != null) {
            try {
                instance = def.getConstructor().newInstance(args);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': '%s'.", def.getName(), def.getBeanClass().getName()), e);
            }
        } else {
            Object configInstance = getBean(def.getFactoryName());
            try {
                instance = def.getFactoryMethod().invoke(configInstance, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': '%s'.", def.getName(), def.getBeanClass().getName()), e);
            }
        }
        def.setInstance(instance);

        // 调用 BeanPostProcessor 的 前置处理，这里感觉可以直接放到 构造方法里面执行，按照 SpringBean 的声明周期来执行
        for (BeanPostProcessor processor : beanPostProcessors) {
            Object processed = processor.postProcessBeforeInitialization(def.getInstance(), def.getName());
            if (def.getInstance() != processed) {
                this.originBeanMap.putIfAbsent(def.getName(), def.getInstance());
                def.setInstance(processed);
            }
        }
        return def.getInstance();
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition beanDefinition = this.beans.get(name);
        if (beanDefinition == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) beanDefinition.getRequiredInstance();
    }

    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(name, requiredType);
        if (t == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s' and type '%s'.", name, requiredType));
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<BeanDefinition> beanDefinitions = findBeanDefinitions(requiredType);
        if (beanDefinitions.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> list = new ArrayList<>(beanDefinitions.size());
        for (BeanDefinition beanDefinition : beanDefinitions) {
            list.add((T) beanDefinition.getRequiredInstance());
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition beanDefinition = findBeanDefinition(requiredType);
        if (beanDefinition == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) beanDefinition.getRequiredInstance();
    }

    /**
     * 检测是否存在指定Name的Bean
     */
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    /**
     * findXxx与getXxx类似，但不存在时返回null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> List<T> findBeans(Class<T> requiredType) {
        return findBeanDefinitions(requiredType).stream().map(def -> (T) def.getRequiredInstance()).collect(Collectors.toList());
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

    Map<String, BeanDefinition> createBeanDefinitions(Set<String> beanClassNames) {
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

    /**
     * 注入属性
     * @param def           Bean定义
     * @param clazz         Bean 的类型
     * @param bean          bean 的实例
     * @throws ReflectiveOperationException     反射异常
     */
    void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws ReflectiveOperationException {
        // 在当前类查找 Field 和 Method 并注入
        for (Field field : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, field);
        }
        for (Method method : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, method);
        }
        // 在父类类查找 Field 和 Method 并注入
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            injectProperties(def, superclass, bean);
        }
    }

    /**
     * 注入单个属性
     * @param def           Bean定义
     * @param clazz         Bean的类型
     * @param bean          Bean的实例
     * @param acc           Bean的成员，字段或者setter方法
     * @throws ReflectiveOperationException     反射异常
     */
    void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws ReflectiveOperationException {
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        // 没有这两个注解则不用注入
        if (value == null && autowired == null) {
            return;
        }

        Field field = null;
        Method method = null;
        // 检查字段、方法不为静态、常态，设置访问权限，方法的参数数量必须为1
        if (acc instanceof Field) {
            Field f = (Field) acc;
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if (acc instanceof Method) {
            Method m = (Method) acc;
            checkFieldOrMethod(m);
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(
                        String.format("Cannot inject a non-setter method %s for bean '%s': %s", m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessibleType = field != null ? field.getType() : method.getParameterTypes()[0];

        // 不能同时标注 @Value 和 @Autowired
        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        // Value 注入
        if (value != null) {
            // 从配置属性解析器中获取属性
            Object propValue = this.propertiesResolver.getRequiredProperty(value.value(), accessibleType);
            if (field != null) {
                log.debug("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, propValue);
                field.set(bean, propValue);
            }
            if (method != null) {
                log.debug("Method injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, propValue);
                method.invoke(bean, propValue);
            }
        }

        // Autowired 注入
        if (autowired != null) {
            String name = autowired.name();
            boolean required = autowired.value();
            Object depends = name.isEmpty() ? findBean(accessibleType) : findBean(name, accessibleType);
            // 检查是否为必须
            if (required && depends == null) {
                throw new UnsatisfiedDependencyException(String.format("Dependency bean not found when inject %s.%s for bean '%s': %s", clazz.getSimpleName(),
                        accessibleName, def.getName(), def.getBeanClass().getName()));
            }
            if (depends != null) {
                if (field != null) {
                    log.debug("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, depends);
                    field.set(bean, depends);
                }
                if (method != null) {
                    log.debug("Mield injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, depends);
                    method.invoke(bean, depends);
                }
            }
        }
    }

    /**
     * 检查字段或者方法是否可用
     * 如果是静态成员，报错
     * 如果是常量字段，报错
     * 如果是常量方法，警告
     * @param m     字段或方法
     */
    void checkFieldOrMethod(Member m) {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new BeanDefinitionException("Cannot inject static field: " + m);
        }
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field) {
                throw new BeanDefinitionException("Cannot inject final field: " + m);
            }
            if (m instanceof Method) {
                log.warn("Inject final method should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException.");
            }
        }
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
        if (primaryDefs.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }

    @Override
    public void close() {
        log.info("Closing {}...", this.getClass().getName());
        this.beans.values().forEach(def -> {
            final Object beanInstance = getProxiedInstance(def);
            callMethod(beanInstance, def.getDestroyMethod(), def.getDestroyMethodName());
        });
        this.beans.clear();
        log.info("{} closed.", this.getClass().getName());
        ApplicationContextUtils.setApplicationContext(null);
    }
}
