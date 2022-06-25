package com.hankun.parent.mq.consumer;

import com.hankun.parent.mq.message.KunReceiveMessage;

import java.util.List;

/**
 * 实现此接口进行消费者监听
 * @author hankun
 */
public interface ConsumerListener<T> {

    /**
     * 配置
     *
     * @return
     */
    default ConsumerListenerConfig config() {
        return ConsumerListenerConfig.builder().idempotent(true).build();
    }

    /**
     * 订阅对象
     *
     * @return
     */
    String subscribeTopic();


    /**
     * 订阅tags
     *
     * @return
     */
    String subscribeTags();

    /**
     * 推送消息回调
     *
     * @param list
     * @return
     */
    boolean receive(List<KunReceiveMessage<T>> list);
}
