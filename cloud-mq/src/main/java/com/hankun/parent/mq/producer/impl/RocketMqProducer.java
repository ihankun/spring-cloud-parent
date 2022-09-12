package com.hankun.parent.mq.producer.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.hankun.parent.commons.context.DomainContext;
import com.hankun.parent.commons.context.GrayContext;
import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;
import com.hankun.parent.commons.id.IdGenerator;
import com.hankun.parent.commons.utils.ServerStateUtil;
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
import com.hankun.parent.mq.producer.ban.TopicConfigInfo;
import com.hankun.parent.mq.rocketmq.RocketMqAdmin;
import com.hankun.parent.mq.rocketmq.producer.KunMqProducer;
import com.hankun.parent.mq.rocketmq.producer.strategy.KunGrayStrategy;
import com.hankun.parent.mq.rocketmq.producer.strategy.KunProdStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.latency.MQFaultStrategy;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.MDC;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author hankun
 */
@Slf4j
public class RocketMqProducer implements MqProducer {

    DefaultMQProducer producer;

    MqProperties mqProperties;

    private static final ConcurrentHashMap<String, Boolean> TOPIC_IS_READY = new ConcurrentHashMap();

    private String grayMark;


    @Override
    public void init(MqProperties config) {

        this.mqProperties = config;
        String producerGroupName = SpringHelpers.context().getEnvironment().getProperty("spring.application.name", "default_producer_group");
        // 获取当前环境的灰度或者灰灰度标识
        this.grayMark = Optional.ofNullable(ServerStateUtil.getGrayMark()).orElse("false");
        MQFaultStrategy mqFaultStrategy = new MQFaultStrategy();
        if (Boolean.FALSE.toString().equalsIgnoreCase(grayMark)) {
            // 生产节点逻辑：发送消息到queue[n-max]
            mqFaultStrategy = new KunProdStrategy(mqProperties.getGraySize());
        } else if (Boolean.TRUE.toString().equalsIgnoreCase(grayMark)){
            // 灰度节点逻辑：发送消息到queue[0-n]
            // 灰灰度节点，生产消息发送到灰度队列，灰灰度节点不再消费消息
            mqFaultStrategy = new KunGrayStrategy(mqProperties.getGraySize());
        }
        producer = new KunMqProducer(producerGroupName, mqFaultStrategy);
        producer.setInstanceName("producer-" + String.valueOf(UtilAll.getPid()));

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
            // 检查topic是否已经创建，是否按照配置创建
            try {
                this.checkTopicForReady(topic.getTopic());
            } catch (Exception e) {
                log.error("检测topic={}时发生异常e={}", topic, e);
            }

            MDC.put(TraceLogContext.LOG_TYPE, LogTypeEnum.MQ.getValue());
            assertMessage(topic, data);
            log.info("RocketMqProducer.sendMsg.start,topic={},data={}", topic, data);
            KunMqMsg msg = buildMsg(topic, data);
            Message message = new Message(topic.getTopic(), topic.getTags(), msg.serialize().getBytes());
            message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
            message.putUserProperty("grayMark", grayMark);
            message.putUserProperty("graySize", String.valueOf(mqProperties.getGraySize()));
            SendResult response = producer.send(message);
            /*
            producer.send(message, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    return null;
                }
            },gra)*/
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
            // 检查topic是否已经创建，是否按照配置创建
            try {
                this.checkTopicForReady(topic.getTopic());
            } catch (Exception e) {
                log.error("检测topic={}时发生异常e={}", topic, e);
            }

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
                message.putUserProperty("grayMark", grayMark);
                message.putUserProperty("graySize", String.valueOf(mqProperties.getGraySize()));
                list.add(message);
            }
            MessageBatch message = MessageBatch.generateFromList(list);
            message.setBody(message.encode());
            message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
            message.putUserProperty("grayMark", grayMark);
            message.putUserProperty("graySize", String.valueOf(mqProperties.getGraySize()));
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
            // 检查topic是否已经创建，是否按照配置创建
            try {
                this.checkTopicForReady(topic.getTopic());
            } catch (Exception e) {
                log.error("检测topic={}时发生异常e={}", topic, e);
            }

            KunMqMsg msg = buildMsg(topic, data);

            Message message = new Message(topic.getTopic(), topic.getTags(), msg.serialize().getBytes());
            message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
            message.putUserProperty("grayMark", grayMark);
            message.putUserProperty("graySize", String.valueOf(mqProperties.getGraySize()));
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
            // 检查topic是否已经创建，是否按照配置创建
            try {
                this.checkTopicForReady(topic.getTopic());
            } catch (Exception e) {
                log.error("检测topic={}时发生异常e={}", topic, e);
            }

            List<Message> messageList = new ArrayList<>(data.size());
            List<String> messageId = new ArrayList<>(data.size());

            for (T item : data) {
                KunMqMsg msg = buildMsg(topic, item);
                messageId.add(msg.getMessageId());
                Message message = new Message(topic.getTopic(), topic.getTags(), msg.serialize().getBytes());
                message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
                message.putUserProperty("grayMark", grayMark);
                message.putUserProperty("graySize", String.valueOf(mqProperties.getGraySize()));
                messageList.add(message);
            }

            MessageBatch message = MessageBatch.generateFromList(messageList);
            message.setBody(message.encode());

            message.putUserProperty(TraceLogConstant.TRACE_ID, TraceLogConstant.getTraceId());
            message.putUserProperty("grayMark", grayMark);
            message.putUserProperty("graySize", String.valueOf(mqProperties.getGraySize()));
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
            if (org.springframework.util.StringUtils.isEmpty(gray)) {
                gray = GrayMark.getGrayMark();
                if (MqAutoConfiguration.GRAY_MARK.equals(gray)) {
                    gray = Boolean.TRUE.toString();
                }
                if (org.springframework.util.StringUtils.isEmpty(gray)) {
                    gray = Boolean.FALSE.toString();
                }
            }
            msg.setGray(gray);
            //设置当前登录用户信息
            LoginUserInfo loginUserInfo = LoginUserContext.getLoginUserInfo();
            msg.setLoginUserInfo(loginUserInfo);

            String domain = DomainContext.getCurrentDomain();
            if (org.springframework.util.StringUtils.isEmpty(domain)) {
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

    /**
     * 检查topic是否已经创建，未创建根据参数创建；已创建检查是否和配置一致，不一致则更新topic
     * @param topic
     */
    private void checkTopicForReady(String topic) {
        // 仅在首次发送消息时检查
        boolean isTopicReady = Optional.ofNullable(TOPIC_IS_READY.get(topic)).orElse(false);
        if (!isTopicReady) {
            // 1、启动rocketmq管理工具
            DefaultMQAdminExt mqAdminExt = this.getAndStartMQAdminExt(topic);
            if (mqAdminExt == null) {
                return;
            }
            // 2、检查topic是否存在
            boolean isExisted = RocketMqAdmin.checkTopicExist(mqAdminExt, topic);
            // 如果存在则检查queue配置是否正确
            List<TopicConfigInfo> needCreateOrUpdateTopicConfigInfos = Lists.newArrayList();
            ClusterInfo clusterInfo = RocketMqAdmin.fetchClusterInfo(mqAdminExt);
            if (Objects.isNull(clusterInfo)) {
                log.error(String.format("创建topic[%s]配置信息时无法获取到集群信息！！！", topic));
                mqAdminExt.shutdown();
                return;
            }
            if (isExisted) {
                // 3、获取当前topic相关配置
                List<TopicConfigInfo> topicConfigInfos = RocketMqAdmin.fetchTopicConfig(mqAdminExt, topic, clusterInfo);
                if (topicConfigInfos.isEmpty()) {
                    log.error(String.format("无法获取topic[%s]配置信息！！！", topic));
                    mqAdminExt.shutdown();
                    return;
                }

                // 4、过滤与配置不相同的topic(topic对应的读写队列数量是否相同)
                needCreateOrUpdateTopicConfigInfos.addAll(topicConfigInfos.stream()
                        .filter(topicConfigInfo -> {
                            if (topicConfigInfo.getReadQueueNums() != mqProperties.getReadQueueNums() ||
                                    topicConfigInfo.getWriteQueueNums() != mqProperties.getWriteQueueNums()) {
                                topicConfigInfo.setReadQueueNums(mqProperties.getReadQueueNums());
                                topicConfigInfo.setWriteQueueNums(mqProperties.getWriteQueueNums());
                                topicConfigInfo.setPerm(mqProperties.getPerm());
                                return true;
                            }
                            return false;
                        })
                        .collect(Collectors.toList()));

            } else {
                TopicConfigInfo topicConfigInfo = new TopicConfigInfo();
                // 随机设置选取broker，以便同自动创建的队列保持一致（仅在其中一个broker上创建topic）
                List<String> brokerNameList = RocketMqAdmin.changeToBrokerNameList(clusterInfo, mqProperties);
                Random random = new Random();
                int index = random.nextInt(brokerNameList.size());
                topicConfigInfo.setBrokerNameList(Lists.newArrayList(brokerNameList.get(index)));
                topicConfigInfo.setTopicName(topic);
                topicConfigInfo.setWriteQueueNums(mqProperties.getWriteQueueNums());
                topicConfigInfo.setReadQueueNums(mqProperties.getReadQueueNums());
                topicConfigInfo.setPerm(mqProperties.getPerm());

                needCreateOrUpdateTopicConfigInfos.add(topicConfigInfo);
            }

            if (needCreateOrUpdateTopicConfigInfos.isEmpty()) {
                TOPIC_IS_READY.put(topic, true);
            } else {
                for (TopicConfigInfo needCreateOrUpdateTopicConfigInfo : Optional.ofNullable(needCreateOrUpdateTopicConfigInfos).orElse(Collections.emptyList())) {
                    this.createAndUpdateTopic(topic, mqAdminExt, clusterInfo, needCreateOrUpdateTopicConfigInfo);
                }
            }
            mqAdminExt.shutdown();
        }
    }

    private void createAndUpdateTopic(String topic, DefaultMQAdminExt mqAdminExt, ClusterInfo clusterInfo, TopicConfigInfo needCreateOrUpdateTopicConfigInfo) {
        TopicConfig topicConfig = new TopicConfig();
        BeanUtils.copyProperties(needCreateOrUpdateTopicConfigInfo, topicConfig);
        try {
            mqAdminExt.createAndUpdateTopicConfig(clusterInfo.getBrokerAddrTable().get(needCreateOrUpdateTopicConfigInfo.getBrokerNameList().get(0)).selectBrokerAddr(), topicConfig);

            // 等待topic创建完成(每隔0.1秒检测一次，总共等待1秒钟)
            boolean createResult = false;
            long startTime = System.currentTimeMillis();
            while (!createResult) {
                createResult = RocketMqAdmin.checkTopicExist(mqAdminExt, topic);
                if (System.currentTimeMillis() - startTime < mqProperties.getCreateTopicWaitTime() * 1000) {
                    RocketMqAdmin.waitForMoment(100);
                } else {
                    log.error(String.format("timeout,but create topic[%s] failed!", topic));
                    break;
                }
            }
            if (createResult) {
                TOPIC_IS_READY.put(topic, createResult);
            }
        } catch (Exception e) {
            log.error(String.format("创建或者更新topic[%s]时发生异常！！！", topic), e);
        }
    }

    private DefaultMQAdminExt getAndStartMQAdminExt(String topic) {
        DefaultMQAdminExt mqAdminExt = new DefaultMQAdminExt();
        mqAdminExt.setNamesrvAddr(mqProperties.getUrl());
        try {
            mqAdminExt.start();
        } catch (MQClientException e) {
            log.error(String.format("检测topic[%s]时启动MQAdmin失败！！！", topic), e);
            // 启动失败场景下使用mq服务端自动生成的topic，此处直接返回
            // mqAdminExt.shutdown();
            return null;
        }
        return mqAdminExt;
    }
}
