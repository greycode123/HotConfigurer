package com.cmcc.zyhy.hc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SpringHotConfigurer extends PropertyPlaceholderConfigurer implements HotConfigurer, ApplicationListener<ContextRefreshedEvent> {
    
    private Logger logger = LoggerFactory.getLogger(SpringHotConfigurer.class);
    private Properties props;       // 存取properties配置文件key-value结果
    
    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
            throws BeansException {
        super.processProperties(beanFactory, props);
        this.props = props;
    }
    
    public String getValue(String key) {
        return props.getProperty(key);
    }
    
    private void refreshProp() {
        try {
            logger.info("SpringHotConfigurer refreshProp...");
            Properties mergedProps = mergeProperties();
            convertProperties(mergedProps);
            // TODO 刷新的时候并发问题
            this.props = mergedProps;
        } catch (IOException ex) {
            logger.error("Could not load properties", ex);
        }
    }
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            initConfigRefresh();
        }
    }
    
    private void initConfigRefresh() {
        try {
            Field field = ReflectionUtils.findField(PropertiesLoaderSupport.class, "locations");
            field.setAccessible(true);
            Resource[] locations = (Resource[]) ReflectionUtils.getField(field, this);
            logger.info("=============================================={}" + locations);
            
            List<String> locationList = new ArrayList<>();
            for (Resource location : locations) {
                locationList.add(location.getURL().getFile());
            }
            
            new FileModifedChecker(locationList) {
                @Override
                public void onChange() {
                    refreshProp();
                }
            }.startCheck();
            
        } catch (Exception e) {
            logger.error("===========================================" + e.getMessage());
        }
    }
}