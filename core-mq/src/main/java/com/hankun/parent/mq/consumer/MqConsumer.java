package com.hankun.parent.mq.consumer;

import com.hankun.parent.mq.config.MqProperties;

import java.util.List;

/**
 * @author hankun
 */
public interface MqConsumer {

    /**
     * 初始化消费者
     *
     * @param config
     * @param listenerList
     */
    void initialize(MqProperties config, List<ConsumerListener> listenerList);
}
