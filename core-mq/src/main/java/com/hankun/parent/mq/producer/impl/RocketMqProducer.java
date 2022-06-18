package com.hankun.parent.mq.producer.impl;

import com.alibaba.fastjson.JSON;
import com.hankun.parent.commons.context.DomainContext;
import com.hankun.parent.commons.context.GrayContext;
import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;
import com.hankun.parent.commons.id.IdGenerator;
import com.hankun.parent.commons.utils.SpringHelpers;
import com.hankun.parent.log.constant.TraceLogConstant;
import com.hankun.parent.log.context.TraceLogContext;
import com.hankun.parent.log.enums.LogTypeEnum;
import com.hankun.parent.mq.MqAutoConfiguration;
import com.hankun.parent.mq.config.GrayMark;
import com.hankun.parent.mq.config.MqProperties;
import com.hankun.parent.mq.constants.KunMqSendResult;
import com.hankun.parent.mq.message.KunMqMsg;
import com.hankun.parent.mq.producer.KunMqSendCallback;
import com.hankun.parent.mq.producer.KunTopic;
import com.hankun.parent.mq.producer.MqProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hankun
 */
@Slf4j
public class RocketMqProducer implements MqProducer {

    DefaultMQProducer producer;


    @Override
    public void init(MqProperties config) {

        String producerGroupName = SpringHelpers.context().getEnvironment().getProperty("spring.application.name", "default_producer_group");

        producer = new DefaultMQProducer(producerGroupName);
        producer.setNamesrvAddr(config.getUrl());
        producer.setVipChannelEnabled(false);
        if (config.getTimeOut() != null) {
            producer.setSendMsgTimeout(config.getTimeOut());
        }

        log.info("rocket.mq.producer.init:config={}", JSON.toJSONString(config));

        try {
            producer.start();
        } catch (MQClientException e) {
            log.error(e.getMessage(), e);
        }
        log.info("rocket.mq.producer.start.success:config={}", JSON.toJSONString(config));
    }

    @Override
    public <T> KunMqSendResult sendMsg(KunTopic topic, T data) {
        try {
            MDC.put(TraceLogContext.LOG_TYPE, LogTypeEnum.MQ.getValue());
            assertMessage(topic, data);
            log.info("RocketMqProducer.sendMsg.start,topic={},data={}", topic, data);
            KunMqMsg msg = buildMsg(topic, data);
            Message message = new Message(topic.getTopic(), topic.getTags(), msg.serialize().getBytes());
            message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
            SendResult response = producer.send(message);
            KunMqSendResult result = new KunMqSendResult();
            result.setMessageId(new String[]{response.getMsgId()});
            log.info("RocketMqProducer.sendMsg.finish,result={}", result);
            return result;
        } catch (Exception e) {
            log.error("RocketMqProducer.sendMsg.exception,topic={},data={},e={}", topic, data, e);
            throw new RuntimeException(e.getMessage());
        } finally {
            MDC.remove(TraceLogContext.LOG_TYPE);
        }
    }

    @Override
    public <T> KunMqSendResult sendBatchMsg(KunTopic topic, List<T> data) {
        try {
            MDC.put(TraceLogContext.LOG_TYPE, LogTypeEnum.MQ.getValue());
            assertMessage(topic, data);
            log.info("RocketMqProducer.sendBatchMsg.start,topic={},data={}", topic, data);
            List<Message> list = new ArrayList<>(data.size());
            List<String> messageId = new ArrayList<>(data.size());
            for (T item : data) {
                KunMqMsg msg = buildMsg(topic, item);
                messageId.add(msg.getMessageId());
                Message message = new Message(topic.getTopic(), topic.getTags(), msg.serialize().getBytes());
                message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
                list.add(message);
            }
            MessageBatch message = MessageBatch.generateFromList(list);
            message.setBody(message.encode());
            message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
            producer.send(message);
            KunMqSendResult result = new KunMqSendResult();
            result.setMessageId(messageId.toArray(new String[0]));
            return result;
        } catch (Exception e) {
            log.error("RocketMqProducer.sendBatchMsg.exception,topic={},data={},e={}", topic, data, e);
            throw new RuntimeException(e.getMessage());
        } finally {
            MDC.remove(TraceLogContext.LOG_TYPE);
        }
    }

    @Override
    public <T> void sendAsyncMsg(KunTopic topic, T data, KunMqSendCallback callback) {

        assertMessage(topic, data);

        log.info("RocketMqProducer.sendAsyncMsg.start,topic={},data={}", topic, data);

        try {

            KunMqMsg msg = buildMsg(topic, data);

            Message message = new Message(topic.getTopic(), topic.getTags(), msg.serialize().getBytes());
            message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
            producer.send(message, new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(SendResult resp) {
                    log.info("RocketMqProducer.sendAsyncMsg.onSuccess,topic={},data={},result={}", topic, data, resp);
                    if (callback == null) {
                        return;
                    }
                    KunMqSendResult result = new KunMqSendResult();
                    result.setMessageId(new String[]{msg.getMessageId()});
                    try {
                        callback.success(result);
                    } catch (Exception e) {
                        log.error("RocketMqProducer.sendAsyncMsg.exception,topic={},data={},e={}", topic, data, e);
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("RocketMqProducer.sendAsyncMsg.onException,topic={},data={},e={}", topic, data, throwable);
                    try {
                        callback.exception(throwable);
                    } catch (Exception e) {
                        log.error("RocketMqProducer.sendAsyncMsg.exception2,topic={},data={},e={}", topic, data, e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("RocketMqProducer.sendAsyncMsg.exception3,topic={},data={},e={}", topic, data, e);
            throw new RuntimeException(e.getMessage());
        }

    }

    @Override
    public <T> void sendAsyncBatchMsg(KunTopic topic, List<T> data, KunMqSendCallback callback) {

        assertMessage(topic, data);

        log.info("RocketMqProducer.sendAsyncBatchMsg.start,topic={},data={}", topic, data);

        try {

            List<Message> messageList = new ArrayList<>(data.size());
            List<String> messageId = new ArrayList<>(data.size());

            for (T item : data) {
                KunMqMsg msg = buildMsg(topic, item);
                messageId.add(msg.getMessageId());
                Message message = new Message(topic.getTopic(), topic.getTags(), msg.serialize().getBytes());
                message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
                messageList.add(message);
            }

            MessageBatch message = MessageBatch.generateFromList(messageList);
            message.setBody(message.encode());

            message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
            producer.send(message, new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(SendResult resp) {

                    log.info("RocketMqProducer.sendAsyncBatchMsg.onSuccess,result={}", resp);

                    if (callback == null) {
                        return;
                    }
                    KunMqSendResult result = new KunMqSendResult();
                    result.setMessageId(messageId.toArray(new String[0]));
                    try {
                        callback.success(result);
                    } catch (Exception e) {
                        log.error("RocketMqProducer.sendAsyncBatchMsg.callback.exception,topic={},data={},e={}", topic, data, e);
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("RocketMqProducer.sendAsyncBatchMsg.onException,topic={},data={},e={}", topic, data, throwable);
                    try {
                        callback.exception(throwable);
                    } catch (Exception e) {
                        log.error("RocketMqProducer.sendAsyncBatchMsg.onException.callback,topic={},data={},e={}", topic, data, throwable);
                    }
                }
            });
        } catch (Exception e) {
            log.error("RocketMqProducer.sendAsyncBatchMsg.send.exception,topic={},data={},e={}", topic, data, e);
            throw new RuntimeException(e.getMessage());
        }
    }


    private <T> void assertMessage(KunTopic topic, T data) {
        if (producer == null) {
            throw new RuntimeException("producer is null");
        }

        if (data == null) {
            throw new RuntimeException("message is null");
        }

        if (StringUtils.isEmpty(topic.getTopic())) {
            throw new RuntimeException("message.header.topic is null");
        }

        if (StringUtils.isEmpty(topic.getTags())) {
            throw new RuntimeException("message.header.tags is null");
        }
    }

    private <T> KunMqMsg buildMsg(KunTopic topic, T data) {

        try {

            KunMqMsg msg = new KunMqMsg();
            msg.setData(data);
            msg.setTopic(topic.getTopic());
            msg.setTag(topic.getTags());
            String gray = GrayContext.get();
            if (StringUtils.isEmpty(gray)) {
                gray = GrayMark.getGrayMark();
                if (MqAutoConfiguration.GRAY_MARK.equals(gray)) {
                    gray = Boolean.TRUE.toString();
                }
                if (StringUtils.isEmpty(gray)) {
                    gray = Boolean.FALSE.toString();
                }
            }
            msg.setGray(gray);
            //设置当前登录用户信息
            LoginUserInfo loginUserInfo = LoginUserContext.getLoginUserInfo();
            msg.setLoginUserInfo(loginUserInfo);

            String domain = DomainContext.getCurrentDomain();
            if (StringUtils.isEmpty(domain)) {
                log.error("RocketMqProducer.buildMsg.domain.null(构造MQ消息时未获取到当前登录信息的域名配置信息，请检查业务发送方逻辑)");
            }

            //设置domain，用于Sass化隔离
            msg.setTelnet(domain);

            String msgId = IdGenerator.ins().generator().toString();
            msg.setMessageId(msgId);

            return msg;

        } catch (Exception e) {
            log.error("RocketMqProducer.buildMsg.exception,topic={},data={},e={}", topic, data, e);
            throw e;
        }
    }
}
