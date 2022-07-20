package com.hankun.parent.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author hankun
 */
@Slf4j
@Configuration
@ComponentScan(basePackageClasses = {DbAutoConfiguration.class})
public class DbAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("DbAutoConfiguration.init.start");
    }
}
