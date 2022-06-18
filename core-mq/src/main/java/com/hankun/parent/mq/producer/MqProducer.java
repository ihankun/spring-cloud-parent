package com.hankun.parent.mq.producer;

import com.hankun.parent.mq.config.MqProperties;
import com.hankun.parent.mq.constants.KunMqSendResult;

import java.util.List;

/**
 * @author hankun
 */
public interface MqProducer {

    /**
     * 初始化生产者
     *
     * @param config mq配置
     */
    void init(MqProperties config);


    /**
     * 同步发送单个消息
     *
     * @param topic 使用构造者构建topic与tag
     * @param data  待发送数据
     * @return
     */
    <T> KunMqSendResult sendMsg(KunTopic topic, T data);

    /**
     * 同步批量发送消息
     *
     * @param topic 使用构造者构建topic与tag
     * @param data  待发送数据
     * @return
     */
    <T> KunMqSendResult sendBatchMsg(KunTopic topic, List<T> data);


    /************************** Async Message  ***************************/

    /**
     * 异步发送单个消息
     *
     * @param topic    使用构造者构建topic与tag
     * @param data     待发送数据
     * @param callback 回调方法
     */
    <T> void sendAsyncMsg(KunTopic topic, T data, KunMqSendCallback callback);

    /**
     * 异步批量发送消息
     *
     * @param topic    使用构造者构建topic与tag
     * @param data     待发送数据
     * @param callback 回调方法
     */
    <T> void sendAsyncBatchMsg(KunTopic topic, List<T> data, KunMqSendCallback callback);
}
