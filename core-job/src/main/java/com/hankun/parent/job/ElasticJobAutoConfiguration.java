package com.hankun.parent.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author hankun
 */
@Configuration
@ComponentScan(basePackageClasses = ElasticJobAutoConfiguration.class)
public class ElasticJobAutoConfiguration {
}
