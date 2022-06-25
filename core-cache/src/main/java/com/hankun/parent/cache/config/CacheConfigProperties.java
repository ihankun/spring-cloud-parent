package com.hankun.parent.cache.config;

import org.springframework.boot.autoconfigure.cache.CacheType;

/**
 * 缓存配置属性
 * @author hankun
 */
public class CacheConfigProperties {

    /**
     * 缓存类型 REDIS?MEMORY
     */
    private CacheType type;

    /**
     * 缓存调用地址，如redis的地址
     */
    private String url;
}
