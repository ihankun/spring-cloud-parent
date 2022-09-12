package com.hankun.parent.mq.rocketmq.producer;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.latency.MQFaultStrategy;
import org.apache.rocketmq.client.log.ClientLogger;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * @author hankun
 */
public class KunMqProducer extends DefaultMQProducer {

    private final InternalLogger log = ClientLogger.getLog();
    protected final transient DefaultMQProducerImpl defaultMQProducerImpl;

    public KunMqProducer(String producerGroup, MQFaultStrategy mqFaultStrategy) {
        this(producerGroup, mqFaultStrategy, null);
    }

    public KunMqProducer(String producerGroup, MQFaultStrategy mqFaultStrategy, RPCHook rpcHook) {
        this.setProducerGroup(producerGroup);
        this.defaultMQProducerImpl = new KunMqProducerImpl(this, mqFaultStrategy, rpcHook);
    }

    @Override
    public void start() throws MQClientException {
        this.defaultMQProducerImpl.start();
        if (null != this.getTraceDispatcher()) {
            try {
                this.getTraceDispatcher().start(this.getNamesrvAddr());
            } catch (MQClientException e) {
                log.warn("trace dispatcher start failed ", e);
            }
        }
    }

    @Override
    public SendResult send(
            Message msg) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.defaultMQProducerImpl.send(msg);
    }
}
