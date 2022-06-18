package com.hankun.parent.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author hankun
 */
@Configuration
//@ConfigurationProperties(prefix = "kun")
@ComponentScan(basePackageClasses = CacheAutoConfiguration.class)
public class CacheAutoConfiguration {
}
