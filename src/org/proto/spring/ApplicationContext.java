package org.proto.spring;

import org.proto.spring.annotation.Autowired;
import org.proto.spring.annotation.Component;
import org.proto.spring.annotation.ComponentScan;
import org.proto.spring.annotation.Scope;

import java.beans.Introspector;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author proto
 */
public class ApplicationContext {

    private Class configClass;
    //bean的定义
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    //单例池
    private Map<String, Object> singletonObjects = new HashMap<>();

    public ApplicationContext(Class configClass){

        this.configClass = configClass;

        //扫描所有bean
        scan(configClass);

        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (beanDefinition.getScope().equals("singleton")) {

                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);

            }
        }



    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();

        Object instance = null;
        try {
            instance = clazz.getConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    field.set(instance, getBean(field.getName()));
                }
            }

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }


        return instance;


    }

    public Object getBean(String beanName) {

        if (!beanDefinitionMap.containsKey(beanName)) {
            throw new NullPointerException();
        }

        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

        if (beanDefinition.getScope().equals("singleton")) {
            Object singletonBean = singletonObjects.get(beanName);
            if (singletonBean == null) {
                singletonBean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, singletonBean);
            }
            return singletonBean;
        } else {
            // 原型
            Object prototypeBean = createBean(beanName, beanDefinition);
            return prototypeBean;
        }

    }


    private void scan(Class configClass) {

        if(!configClass.isAnnotationPresent(ComponentScan.class)){
            return;
        }

        //获取扫描路径
        ComponentScan componentScanAnno = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
        String scanPath = componentScanAnno.value();
        scanPath = scanPath.replace('.', '/');

        ClassLoader classLoader = this.configClass.getClassLoader();
        URL resource = classLoader.getResource(scanPath);
        File file = new File(resource.getFile());
        if(!file.isDirectory()){
            return;
        }

        //扫描出适配的class
        doScan(file);

    }

    private void doScan(File file) {

        for(File classPath : file.listFiles()){

            if(classPath.isDirectory()){
                doScan(file);
                continue;
            }

            String absolutePath = classPath.getAbsolutePath();
            absolutePath = absolutePath.substring(absolutePath.indexOf("org"),absolutePath.indexOf(".class"));
            absolutePath = absolutePath.replace('\\', '.');

            //判断是否是个bean spring中使用asm技术，我没那条件
            ClassLoader classLoader = configClass.getClassLoader();
            try {
                Class<?> clazz = classLoader.loadClass(absolutePath);
                if(!clazz.isAnnotationPresent(Component.class)){
                    continue;
                }

                //Bean定义
                BeanDefinition beanDefinition = new BeanDefinition();
                beanDefinition.setType(clazz);
                beanDefinition.setScope(getBeanScope(clazz));
                beanDefinitionMap.put(getBeanName(clazz),beanDefinition);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private String getBeanName(Class<?> clazz) {

        Component component = clazz.getAnnotation(Component.class);
        String beanName = component.value();
        if("".equals(beanName)){
            beanName = Introspector.decapitalize(clazz.getSimpleName());
        }

        return beanName;
    }

    private String getBeanScope(Class<?> clazz) {

        if(clazz.isAnnotationPresent(Scope.class)){
            Scope scope = clazz.getAnnotation(Scope.class);
            return scope.value();
        }else {
            return "singleton";
        }

    }


}
