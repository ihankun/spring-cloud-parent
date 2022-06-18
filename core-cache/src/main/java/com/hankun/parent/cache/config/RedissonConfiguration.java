package com.hankun.parent.cache.config;

import lombok.Getter;
import lombok.Setter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Redisson分布式锁配置类
 * @author hankun
 */
@Setter
@Getter
@Configuration
public class RedissonConfiguration {

    @Resource
    RedisConfig config;

    @Bean
    public RedissonClient getRedissonClient() {

        RedisConfig.Lettuce lettuce = config.getLettuce();
        RedisConfig.Cluster cluster = config.getCluster();
        String password = config.getPassword();
        int database = config.getDatabase();
        Integer port = config.getPort();
        String host = config.getHost();

        Config config = new Config();
        if (cluster != null && !CollectionUtils.isEmpty(cluster.getNodes())) {
            List<String> nodes = new ArrayList<>(cluster.getNodes().size());
            cluster.getNodes().forEach(node -> nodes.add("redis://" + node));
            ClusterServersConfig serversConfig = config.useClusterServers().addNodeAddress(nodes.toArray(new String[nodes.size()]));
            if (!StringUtils.isEmpty(password)) {
                serversConfig.setPassword(password);
            }
            if (lettuce != null && lettuce.getPool() != null) {
                RedisConfig.Pool pool = lettuce.getPool();
                serversConfig.setMasterConnectionMinimumIdleSize(pool.getMinIdle());
                serversConfig.setMasterConnectionPoolSize(pool.getMaxIdle());
                serversConfig.setSlaveConnectionMinimumIdleSize(pool.getMinIdle());
                serversConfig.setSlaveConnectionPoolSize(pool.getMaxIdle());
            }
        } else {
            SingleServerConfig singleServerConfig = config.useSingleServer().setAddress("redis://" + host + ":" + port).setDatabase(database).setConnectionMinimumIdleSize(8);
            if (!StringUtils.isEmpty(password)) {
                config.useSingleServer().setPassword(password);
            }
            if (lettuce != null && lettuce.getPool() != null) {
                RedisConfig.Pool pool = lettuce.getPool();
                singleServerConfig.setConnectionMinimumIdleSize(pool.getMinIdle());
                singleServerConfig.setConnectionPoolSize(pool.getMaxActive());
            }
        }
        return Redisson.create(config);
    }
}
