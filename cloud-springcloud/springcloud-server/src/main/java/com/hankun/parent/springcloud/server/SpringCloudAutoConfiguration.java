package com.hankun.parent.springcloud.server;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = SpringCloudAutoConfiguration.class)
public class SpringCloudAutoConfiguration {
}
