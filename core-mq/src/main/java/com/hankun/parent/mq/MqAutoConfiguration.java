package com.hankun.parent.mq;

import com.hankun.parent.commons.thread.NamedThreadFactory;
import com.hankun.parent.commons.utils.SpringHelpers;
import com.hankun.parent.mq.config.GrayMark;
import com.hankun.parent.mq.config.MqProperties;
import com.hankun.parent.mq.constants.MqTypeEnum;
import com.hankun.parent.mq.consumer.ConsumerListener;
import com.hankun.parent.mq.consumer.MqConsumer;
import com.hankun.parent.mq.producer.MqProducer;
import com.hankun.parent.mq.producer.impl.KafkaMqProducer;
import com.hankun.parent.mq.producer.impl.RocketMqProducer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author hankun
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "kun")
@ComponentScan(basePackageClasses = MqAutoConfiguration.class)
public class MqAutoConfiguration implements ApplicationContextAware {

    private static final String META_MARK = "mark";

    public static final String GRAY_MARK = "gray";

    public MqProperties mq;

    private ApplicationContext context;

    private List<String> loadedListenerList = new ArrayList<>(16);

    public static final int UPDATE_TIME = 5;

    @Resource
    private NacosPropertiesLoader nacosPropertiesLoader;

    @PostConstruct
    public void initConsumer() {
        if (mq == null) {
            log.info("mq.config is null");
            return;
        }
        boolean enable = mq.getConsumer() != null && mq.getConsumer().isEnable();
        if (!enable) {
            log.warn("mq.consumer.enable=false");
            return;
        }


        //开启定时扫描任务
        new NamedThreadFactory("mq.consumer.scan").newThread(() -> {
            int time = 0;
            while (true) {
                try {
                    scanConsumerListener();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                time++;
                if (time >= UPDATE_TIME) {
                    updateNacos();
                    time = 0;
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }

            }
        }).start();


    }

    private void updateNacos() {
        Map<String, String> properties = nacosPropertiesLoader.getProperties();
        if (CollectionUtils.isEmpty(properties)) {
            log.error("MqAutoConfiguration.updateNacos.get.instance.fail,localIp={},gray={}", nacosPropertiesLoader.getLocalIps(), GrayMark.getGrayMark());
        }
        String meta = properties.get(META_MARK);
        GrayMark.setGrayMark(meta);
    }

    /**
     * 扫描动态加入的listener
     */
    private void scanConsumerListener() {

        String type = mq.getType();

        List<ConsumerListener> listenerList = fetchListener();
        if (listenerList == null || listenerList.size() == 0) {
            return;
        }

        try {
            MqConsumer consumer = context.getBean(type + "Consumer", MqConsumer.class);
            consumer.initialize(mq, listenerList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.info("MqConfiguration.scanConsumerListener.init,listenerList={}", listenerList);
    }

    @Bean
    public MqProducer getMqProducer() {

        if (mq == null) {
            log.info("mq.config is null");
            return null;
        }
        boolean enable = mq.getProducer() != null && mq.getProducer().isEnable();
        if (!enable) {
            log.warn("mq.producer.enable=false");
            return null;
        }

        String type = mq.getType();

        MqProducer producer = null;

        if (MqTypeEnum.ROCKETMQ.getType().equals(type)) {

            producer = new RocketMqProducer();

        } else if (MqTypeEnum.KAFKA.getType().equals(type)) {

            producer = new KafkaMqProducer();

        } else {
            log.error("can't find this kind producer implement of type +" + type);
        }

        producer.init(mq);

        return producer;
    }


    private List<ConsumerListener> fetchListener() {
        if (context == null) {
            log.error("ApplicationContext is null");
            return null;
        }

        Map<String, ConsumerListener> consumers;

        try {
            consumers = context.getBeansOfType(ConsumerListener.class);
        } catch (Exception e) {
            return null;
        }

        if (consumers == null || consumers.size() == 0) {
            return null;
        }
        List<ConsumerListener> listenerList = new ArrayList<>(16);
        consumers.forEach((key, value) -> {
            if (!loadedListenerList.contains(key)) {
                listenerList.add(value);
                loadedListenerList.add(key);
            }
        });

        return listenerList;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
        SpringHelpers.setContext(this.context);
    }

}
