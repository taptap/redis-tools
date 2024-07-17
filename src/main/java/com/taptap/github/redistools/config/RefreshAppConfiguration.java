package com.taptap.github.redistools.config;

import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * @author kl (http://kailing.pub)
 * @since 2021/1/4
 */
@Configuration
public class RefreshAppConfiguration implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @ApolloConfigChangeListener
    private void onConfigChange(ConfigChangeEvent changeEvent) {
        Set<String> changedKeys = changeEvent.changedKeys();
        this.applicationContext.publishEvent(new EnvironmentChangeEvent(changedKeys));
        RedisCopyJobConfig jobConfig = applicationContext.getBean(RedisCopyJobConfig.class);
        logger.info("changedKeys: {}", changedKeys);
        logger.info("refresh configuration: {}", jobConfig);
    }
}
