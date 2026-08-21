package com.ibm.websphere.samples.daytrader.web;

import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringEndpointConfigurator
    extends ServerEndpointConfig.Configurator
    implements ApplicationContextAware {

    private static AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext ctx)
        throws BeansException {
        beanFactory = ctx.getAutowireCapableBeanFactory();
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass)
        throws InstantiationException {
        // Let Spring construct the endpoint (so constructor injection works)
        return beanFactory.createBean(endpointClass);
    }
}
