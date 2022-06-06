package com.hankun.parent.db.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Data
@RefreshScope
@Configuration
@ConfigurationProperties(value = "kun.database.config")
public class DbConfig {
    private int maxRows = 10000;
    private boolean checkSql = true;
}
