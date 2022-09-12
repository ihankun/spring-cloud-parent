package com.hankun.parent.mq.rocketmq.producer;

import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.client.latency.MQFaultStrategy;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;

public class KunMqProducerImpl extends DefaultMQProducerImpl {

    private MQFaultStrategy mqFaultStrategy;

    public KunMqProducerImpl(final DefaultMQProducer defaultMQProducer, MQFaultStrategy mqFaultStrategy, RPCHook rpcHook) {
        super(defaultMQProducer, rpcHook);
        this.mqFaultStrategy = mqFaultStrategy;
    }

    @Override
    public MessageQueue selectOneMessageQueue(final TopicPublishInfo tpInfo, final String lastBrokerName) {
        return this.mqFaultStrategy.selectOneMessageQueue(tpInfo, lastBrokerName);
    }
}
