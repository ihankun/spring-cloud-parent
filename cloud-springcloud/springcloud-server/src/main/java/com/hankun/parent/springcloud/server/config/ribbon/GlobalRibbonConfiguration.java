//package com.hankun.parent.springcloud.server.config.ribbon;
//
//import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.cloud.netflix.ribbon.RibbonClient;
//import org.springframework.cloud.netflix.ribbon.RibbonClients;
//import org.springframework.context.annotation.Configuration;
//
////@Configuration
////@ConditionalOnClass(NacosDiscoveryProperties.class)
////@RibbonClients(value = {@RibbonClient(name = "nacosMetaDataRibbonRule", configuration = NacosMetaDataRibbonRule.class),}, defaultConfiguration = GlobalRibbonConfiguration.class)
//public class GlobalRibbonConfiguration {
//}
