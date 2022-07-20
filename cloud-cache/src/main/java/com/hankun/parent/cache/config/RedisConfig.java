package com.hankun.parent.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author hankun
 */
@Data
@Configuration
@ConfigurationProperties("spring.redis")
public class RedisConfig {
    /**
     * 地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 密码
     */
    private String password;

    /**
     * 数据库
     */
    private int database;

    /**
     * 集群配置信息
     */
    private Cluster cluster;

    private Lettuce lettuce;


    @Data
    public static class Lettuce {
        private Pool pool;
    }

    @Data
    public static class Pool {
        private int maxIdle;
        private int maxActive;
        private int minIdle;
    }

    @Data
    public static class Cluster {
        private List<String> nodes;
    }
}
