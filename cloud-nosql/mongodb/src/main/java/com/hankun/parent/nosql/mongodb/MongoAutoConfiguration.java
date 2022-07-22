package com.hankun.parent.nosql.mongodb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
@ComponentScan(basePackageClasses = MongoAutoConfiguration.class)
public class MongoAutoConfiguration {

    @PostConstruct
    public void initMongo() {
        log.info("MongoAutoConfiguration.init.start");
    }

}
