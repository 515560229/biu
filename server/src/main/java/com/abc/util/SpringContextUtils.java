package com.abc.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringContextUtils {
    public static SpringContextUtils INSTANCE;

    @Autowired
    private ApplicationContext context;

    public SpringContextUtils() {
        INSTANCE = this;
    }

    public ApplicationContext getContext() {
        return context;
    }

    public <T> T getBean(String beanName, Class<T> requiredType) {
        return context.getBean(beanName, requiredType);
    }

    public <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }
}
