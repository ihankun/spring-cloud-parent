package com.hankun.parent.springcloud.server.config.ribbon;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.ribbon.ExtendBalancer;
import com.alibaba.cloud.nacos.ribbon.NacosServer;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.hankun.parent.commons.context.GrayContext;
import com.hankun.parent.log.context.TraceLogContext;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
//@Component
//@Scope(value = "prototype")
//@ConditionalOnClass(NacosDiscoveryProperties.class)
public class NacosMetaDataRibbonRule extends AbstractLoadBalancerRule {

    @Resource
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    private static final String META_DATA_VERSION = "mark";

    private static final String GRAY_MARK = "gray";

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {

    }

    @Override
    public Server choose(Object key) {

        // 服务名称
        String serviceName = ((BaseLoadBalancer) getLoadBalancer()).getName();

        String grayStr = GrayContext.get();

        log.info("NacosMetaDataRibbonRule.choose.start,traceId={},key={},serviceName={},gray={}", TraceLogContext.get(), key, serviceName, grayStr);

        boolean gray = StringUtils.isEmpty(grayStr) ? false : (Boolean.valueOf(grayStr));

        return chooseInstance(serviceName, gray);
    }


    /**
     * 根据服务名称和目标版本获取目标服务
     *
     * @param serviceName
     * @param gray
     * @return
     */
    private NacosServer chooseInstance(String serviceName, boolean gray) {
        try {

            // 获取所有被调用服务
            List<Instance> allInstances = nacosDiscoveryProperties.namingServiceInstance().getAllInstances(serviceName);

            if (CollectionUtils.isEmpty(allInstances)) {
                return null;
            }

            //灰度服务列表
            List<Instance> grayInstances = new ArrayList<>(1);
            //正式服务列表
            List<Instance> prodInstances = new ArrayList<>(2);

            for (Instance instance : allInstances) {
                String meta = instance.getMetadata().get(META_DATA_VERSION);
                if (StringUtils.isNotEmpty(meta) || GRAY_MARK.equalsIgnoreCase(meta)) {
                    grayInstances.add(instance);
                } else {
                    prodInstances.add(instance);
                }
            }


            //如果为正式流量，尝试找正式服务列表，找不到，则尝试找灰度列表
            if (!gray) {
                Instance instance = ExtendBalancer.getHostByRandomWeight2(prodInstances);
                if (instance == null) {
                    log.info("NacosMetaDataRibbonRule.chooseInstance.prod.not.found,will.random.from.gray,traceId={},serviceName={},gray.instance.size={}", TraceLogContext.get(), serviceName, grayInstances.size());
                    instance = ExtendBalancer.getHostByRandomWeight2(grayInstances);
                }
                log.info("NacosMetaDataRibbonRule.chooseInstance.prod,traceId={},serviceName={},instance={}", TraceLogContext.get(), serviceName, (instance != null ? (instance.getIp() + ":" + instance.getPort()) :
                        "null"));
                return new NacosServer(instance);
            }


            //灰度流量，尝试先找灰度服务列表，如果找不到，则再尝试找正式服务列表
            Instance instance = ExtendBalancer.getHostByRandomWeight2(grayInstances);
            if (instance == null) {
                log.info("NacosMetaDataRibbonRule.chooseInstance.gray.not.found,will.random.from.prod,traceId={},serviceName={},prod.instance.size={}", TraceLogContext.get(), serviceName, prodInstances.size());
                instance = ExtendBalancer.getHostByRandomWeight2(prodInstances);
            }
            log.info("NacosMetaDataRibbonRule.chooseInstance.gray,traceId={},serviceName={},instance={}", TraceLogContext.get(), serviceName, (instance != null ? (instance.getIp() + ":" + instance.getPort()) : "null"));
            return new NacosServer(instance);


        } catch (NacosException e) {
            log.error("自定义服务发现策略异常:未找到匹配的服务信息,traceId={},错误信息:{}", TraceLogContext.get(), e);
            return null;
        }
    }
}
