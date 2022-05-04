package com.hankun.parent.requestlock;


import com.hankun.parent.cache.CacheBuilder;
import com.hankun.parent.cache.CacheManager;
import com.hankun.parent.cache.enums.CacheType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;

/**
 * @author hankun
 */
@Slf4j
@Configuration
@ComponentScan(basePackageClasses = {RequestlockAutoConfiguration.class})
public class RequestlockAutoConfiguration {

    /**
     * 为请求锁的接口声明通用的缓存管理器
     *
     * @return
     */
    @ConditionalOnMissingBean
    @Order
    @Bean
    public CacheManager<String, String> msunRequestLockCacheManager() {
        return CacheBuilder.build(CacheType.REDIS);
    }

    /**
     * init
     */
    @PostConstruct
    public void init() {
        log.info("RequestLockAutoConfiguration.init.start");
    }

}
