package com.hankun.parent.springcloud.server.util;

import com.alibaba.cloud.nacos.ConditionalOnNacosDiscoveryEnabled;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.hankun.parent.commons.utils.DateUtil;
import com.hankun.parent.commons.utils.ServerStateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnNacosDiscoveryEnabled
public class NacosInfoHolder {

    @Bean
    public NacosDiscoveryProperties nacosProperties() {
        //更改服务详情中的元数据，增加服务注册时间
        NacosDiscoveryProperties properties = new NacosDiscoveryProperties();
        properties.getMetadata().put("startup.time", DateUtil.getNowDate().toString());
        properties.getMetadata().put("version", ServerStateUtil.getVersion());

        //如果启动时存在mark的话，设置到nacos中
        if (!StringUtils.isEmpty(ServerStateUtil.getGrayMark()) && !Boolean.FALSE.toString().equalsIgnoreCase(ServerStateUtil.getGrayMark())) {
            if (Boolean.TRUE.toString().equalsIgnoreCase(ServerStateUtil.getGrayMark())) {
                properties.getMetadata().put("mark", "gray");
            } else {
                properties.getMetadata().put("mark", ServerStateUtil.getGrayMark());
            }
        }
        log.info("NacosInfoHolder.nacosProperties.init.with.data,version={},gray={}", ServerStateUtil.getVersion(), ServerStateUtil.getGrayMark());
        return properties;
    }
}
