package beans;

import beans.domain.Bean;
import beans.repositories.BeanRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;

@Service
public class DatabasePopulator implements ApplicationContextAware {
    @Autowired
    private Neo4jTemplate template;
    @Autowired
    private BeanRepository beansRepository;
    private AnnotationConfigEmbeddedWebApplicationContext ctx;

    @Transactional
    public void list() {
        for (Bean bean : beansRepository.findAll()) {
            template.fetch(bean.getDependencies());
            System.out.println(bean);
        }
    }

    @Transactional
    public void initialize() {
        Map<String, Bean> beans = new LinkedHashMap<>();
        Map<Class, BeanDefinition> beanClasses = new LinkedHashMap<>();
        for (String name : ctx.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = ctx.getBeanDefinition(name);
            beanClasses.put(getClass(beanDefinition), beanDefinition);
        }
        for (String name : ctx.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = ctx.getBeanDefinition(name);

            Bean bean = new Bean(name,
                    beanDefinition.getBeanClassName(),
                    beanDefinition.isSingleton() ? Bean.Scope.singleton : Bean.Scope.prototype);

            MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
            Set<String> autowiredTypes = getAutowiredTypes(beanDefinition.getBeanClassName(), beanClasses);
            Set<String> dependencies = new HashSet<>(autowiredTypes);
            if (beanDefinition.getDependsOn() != null) {
                dependencies.addAll(asList(beanDefinition.getDependsOn()));
            }
            for (PropertyValue value : propertyValues.getPropertyValueList()) {
                if (value.getValue() instanceof BeanDefinition) {
                    dependencies.add(((BeanDefinition) value.getValue()).getBeanClassName());
                }
            }
            for (ConstructorArgumentValues.ValueHolder value : beanDefinition.getConstructorArgumentValues().getGenericArgumentValues()) {
                if (value.getValue() instanceof BeanDefinition)
                    dependencies.add(((BeanDefinition) value.getValue()).getBeanClassName());
            }
            Set<Bean> beanDependencies = new HashSet<>();
            for (String dependency : dependencies) {
                Bean dep = beans.get(dependency);
                if (dep != null) {
                    beanDependencies.add(dep);
                    continue;
                }
                dep = beansRepository.findByType(dependency);
                if (dep != null) {
                    beanDependencies.add(dep);
                    beans.put(dep.getName(), dep);
                    continue;
                }
                dep = template.save(new Bean(dependency, dependency, Bean.Scope.singleton));
                beanDependencies.add(dep);
                beans.put(dep.getName(), dep);
            }
            bean.setDependencies(beanDependencies);
            beans.put(name, template.save(bean));
        }
    }

    private Class<?> getClass(BeanDefinition beanDefinition) {
        try {
            if (beanDefinition == null || beanDefinition.getBeanClassName() == null) return null;
            return Class.forName(beanDefinition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Class findMatchingBeanClass(Class type, Map<Class, BeanDefinition> beanClasses) {
        if (type == null) return null;
        if (beanClasses.containsKey(type)) return type;
        for (Class beanClass : beanClasses.keySet()) {
            if (beanClass == null) continue;
            if (type.isAssignableFrom(beanClass)) return beanClass;
        }
        return null;
    }

    private Set<String> getAutowiredTypes(String beanClassName, Map<Class, BeanDefinition> beanClasses) {
        Set<String> result = new HashSet<>();
        if (beanClassName == null) return result;
        try {
            Class<?> type = Class.forName(beanClassName);
            while (type != null) {
                for (Field field : type.getDeclaredFields()) {
                    Class matchingBeanClass = findMatchingBeanClass(field.getType(), beanClasses);
                    if (matchingBeanClass != null) {
                        result.add(matchingBeanClass.getName());
                        continue;
                    }
                    if (field.isAnnotationPresent(Autowired.class)) {
                        result.add(field.getType().getName());
                    }
                }
                for (Method method : type.getDeclaredMethods()) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    Annotation[][] annotations = method.getParameterAnnotations();
                    for (int i = 0; i < parameterTypes.length; i++) {
                        Class<?> parameterType = parameterTypes[i];
                        Class matchingBeanClass = findMatchingBeanClass(parameterType, beanClasses);
                        if (matchingBeanClass != null) {
                            result.add(matchingBeanClass.getName());
                            continue;
                        }
                        for (Annotation annotation : annotations[i]) {
                            if (annotation.annotationType() == Autowired.class) {
                                result.add(parameterType.getName());
                            }
                        }
                    }
                }

                for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    Annotation[][] annotations = constructor.getParameterAnnotations();
                    for (int i = 0; i < parameterTypes.length; i++) {
                        Class<?> parameterType = parameterTypes[i];
                        Class matchingBeanClass = findMatchingBeanClass(parameterType, beanClasses);
                        if (matchingBeanClass != null) {
                            result.add(matchingBeanClass.getName());
                            continue;
                        }
                        for (Annotation annotation : annotations[i]) {
                            if (annotation.annotationType() == Autowired.class) {
                                result.add(parameterType.getName());
                            }
                        }
                    }
                }
                type = type.getSuperclass();
            }
        } catch (Exception e) {
            throw new RuntimeException("error checking autowired for " + beanClassName, e);
        }
        return result;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = (AnnotationConfigEmbeddedWebApplicationContext) applicationContext;
    }

    public void clean() {
        template.query("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r", null);
    }
}
