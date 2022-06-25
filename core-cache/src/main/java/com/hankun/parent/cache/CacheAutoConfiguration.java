package com.hankun.parent.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author hankun
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "kun")
@ComponentScan(basePackageClasses = CacheAutoConfiguration.class)
public class CacheAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("CacheAutoConfiguration.init.start");
    }
}
