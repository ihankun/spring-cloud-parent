package com.hankun.parent.nosql.mongodb.config;

import com.mongodb.WriteConcern;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;

import java.util.List;

/**
 * @author hankun
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class MongoConfiguration {

    private String uri;
    private String database;

    @Value("#{'${kun.mongodb.database}'.split(',')}")
    private List<String> msunMongoDatabase;

    @Bean
    MongoDbFactory mongoDbFactory() {
        if (StringUtils.isBlank(database)) {
            throw new RuntimeException("请配置spring.data.mongodb.database");
        }

        if (! msunMongoDatabase.contains(database)) {
            throw new RuntimeException("数据库配置不存在！");
        }

        uri = uri.replace("database", database);
        return (MongoDbFactory) new SimpleMongoClientDbFactory(uri);
    }

    @Primary
    @Bean(name = "mongoTemplate")
    public MongoTemplate getMongoTemplate() {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory());
        mongoTemplate.setWriteConcern(WriteConcern.ACKNOWLEDGED);
        return mongoTemplate;
    }
}
