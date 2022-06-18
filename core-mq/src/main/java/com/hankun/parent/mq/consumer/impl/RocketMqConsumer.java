package com.hankun.parent.mq.consumer.impl;

import com.alibaba.fastjson.JSON;
import com.hankun.parent.commons.context.DomainContext;
import com.hankun.parent.commons.context.GrayContext;
import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;
import com.hankun.parent.commons.utils.SpringHelpers;
import com.hankun.parent.log.constant.TraceLogConstant;
import com.hankun.parent.log.context.TraceLogContext;
import com.hankun.parent.log.enums.LogTypeEnum;
import com.hankun.parent.mq.config.GrayMark;
import com.hankun.parent.mq.config.MqProperties;
import com.hankun.parent.mq.consumer.AbstractConsumer;
import com.hankun.parent.mq.consumer.ConsumerListener;
import com.hankun.parent.mq.consumer.ConsumerListenerConfig;
import com.hankun.parent.mq.consumer.MqConsumer;
import com.hankun.parent.mq.message.KunMqMsg;
import com.hankun.parent.mq.message.KunReceiveMessage;
import com.hankun.parent.mq.producer.KunTopic;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hankun
 */
@Component("rocketmqConsumer")
@Slf4j
public class RocketMqConsumer extends AbstractConsumer implements MqConsumer {

    private static final String RETRY = "RETRY";

    public Map<String, DefaultMQPushConsumer> consumers = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("RocketMqConsumer.init");
    }

    /**
     * 监听初始化，遍历所有实现ConsumerListener接口的实现，对不同的topic+tags进行注册
     * 注意：统一个topic下不同tags的消费，要设置不同的消费组，否则会发生消费异常
     *
     * @param config       配置
     * @param listenerList 监听器
     */
    @Override
    public void initialize(MqProperties config, List<ConsumerListener> listenerList) {
        if (listenerList == null || listenerList.size() == 0) {
            log.error("listenerList is null,place implement the interface 'ConsumerListener' and add it to spring " +
                    "ApplicationContext");
            return;
        }

        for (ConsumerListener<?> listener : listenerList) {
            String consumerGroupName = getGroupName(listener);
            buildConsumer(config, consumerGroupName, listener);
        }
    }

    private void buildConsumer(MqProperties config, String consumerGroupName, ConsumerListener<?> listener) {
        if (consumers.containsKey(consumerGroupName)) {
            log.info("RocketMqConsumer.buildConsumer.prod.consumer.exists!name={}", consumerGroupName);
            return;
        }
        DefaultMQPushConsumer consumer = buildConsumerTask(config, consumerGroupName, listener);
        consumers.put(consumerGroupName, consumer);
        log.info("RocketMqConsumer.buildConsumer.build.consumer!name={}", consumerGroupName);
    }


    private DefaultMQPushConsumer buildConsumerTask(MqProperties config, String consumerGroupName, ConsumerListener listener) {
        String subscribeTags = GrayMark.buildConsumerTags(listener.subscribeTags());
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroupName);
        consumer.getDefaultMQPushConsumerImpl().doRebalance();
        consumer.setNamesrvAddr(config.getUrl());
        consumer.setVipChannelEnabled(false);
        if (config.getTimeOut() != null) {
            consumer.setConsumeTimeout(config.getTimeOut());
        }
        try {
            consumer.subscribe(listener.subscribeTopic(), subscribeTags);
        } catch (MQClientException e) {
            log.error(e.getMessage(), e);
            return null;
        }

        RocketMqConsumer point = this;
        consumer.registerMessageListener((MessageListenerConcurrently) (list, context) -> {

            MDC.put(TraceLogContext.LOG_TYPE, LogTypeEnum.MQ.getValue());
            String topic = context.getMessageQueue().getTopic();
            log.info("RocketMqConsumer.received,topic={},list={}", topic, list);
            //构造MQ消息
            List<KunMqMsg> mqMsgs = convertMessage(list);

            if (!CollectionUtils.isEmpty(mqMsgs)) {
                //设置上下文域名信息，用于接收数据后能够自动切换数据源
                String telnet = mqMsgs.get(0).getTelnet();
                if (!StringUtils.isEmpty(telnet)) {
                    DomainContext.mock(telnet);
                }

                //设置上下文登录用户信息
                LoginUserInfo loginUserInfo = mqMsgs.get(0).getLoginUserInfo();
                if (loginUserInfo != null) {
                    LoginUserContext.mock(loginUserInfo);
                }

                String gray = mqMsgs.get(0).getGray();
                GrayContext.mock(gray);
                log.info("RocketMqConsumer.context.info=[telnet={},gray={}]", telnet, gray);
            }

            //构造Receive消息
            List<KunReceiveMessage> msgList = convertToReceiveMessage(listener, point, listener.config(), mqMsgs);
            //回调
            try {
                boolean result = true;
                if (!CollectionUtils.isEmpty(msgList)) {
                    result = listener.receive(msgList);
                }
                //消费成功
                if (result) {
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                //消费失败
                if (listener.config() != null && listener.config().isIdempotent()) {
                    for (KunReceiveMessage message : msgList) {
                        resetConsumed(getIdempotentKey(listener.config(), message.getMessageId(), message.getData()));
                    }
                }
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            } finally {
                MDC.remove(TraceLogContext.LOG_TYPE);
                TraceLogContext.reset();
                DomainContext.clear();
                LoginUserContext.clear();
                GrayContext.clear();
            }
        });
        log.info("RocketMqConsumer.buildConsumerTask,consumer={},groupName={},topic={},tags={}", listener.getClass().getSimpleName(), consumerGroupName, listener.subscribeTopic(), subscribeTags);
        try {
            consumer.start();
            consumer.getDefaultMQPushConsumerImpl().doRebalance();
            return consumer;
        } catch (MQClientException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private String getGroupName(ConsumerListener<?> listener) {
        Environment environment = SpringHelpers.context().getEnvironment();
        String consumerGroupName = environment.getProperty("spring.application.name", "default_consumer_group");
        return consumerGroupName + listener.subscribeTopic() + listener.subscribeTags();
    }

    /**
     * 数据格式转换
     *
     * @param list
     * @return
     */
    private List<KunMqMsg> convertMessage(List<MessageExt> list) {
        List<KunMqMsg> result = new ArrayList<>(list.size());
        for (MessageExt messageExt : list) {

            String body = new String(messageExt.getBody(), Charset.defaultCharset());

            //设置traceID
            String traceId = messageExt.getUserProperty(TraceLogConstant.TRACE_ID);
            TraceLogConstant.setTraceId(StringUtils.isEmpty(traceId) ? TraceLogConstant.getTraceId() : traceId);
            KunMqMsg msg = JSON.parseObject(body, KunMqMsg.class);
            result.add(msg);
        }
        return result;
    }


    /**
     * 构造返回消息
     *
     * @param listener
     * @param point
     * @param config
     * @param list
     * @return
     */
    private List<KunReceiveMessage> convertToReceiveMessage(ConsumerListener listener, RocketMqConsumer point, ConsumerListenerConfig config, List<KunMqMsg> list) {

        List<KunReceiveMessage> messageList = new ArrayList<>(list.size());
        for (KunMqMsg msg : list) {

            KunReceiveMessage receive = new KunReceiveMessage();

            receive.setData(objectToClass(msg.getData(), listener));

            receive.setMessageId(msg.getMessageId());
            receive.setTopic(KunTopic.builder().topic(msg.getTopic()).tags(msg.getTag()).build());
            //处理幂等
            if (config != null && config.isIdempotent()) {
                //如果未设置幂等key，则以消息id为key
                String idempotentKey = getIdempotentKey(config, msg.getMessageId(), msg.getData());
                boolean beConsumed = checkConsumed(idempotentKey);
                receive.setBeConsumed(beConsumed);
                if (!beConsumed) {
                    point.confirmConsumed(idempotentKey);
                }
            }
            messageList.add(receive);

        }
        return messageList;
    }
}
