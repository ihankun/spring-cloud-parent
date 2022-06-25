package com.hankun.parent.mq.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息队列类型枚举
 * @author hankun
 */

@AllArgsConstructor
@Getter
public enum MqTypeEnum {

    /**
     * rocketMq
     */
    ROCKETMQ("rocketmq", "RocketMQ"),
    /**
     * kafka
     */
    KAFKA("kafka", "Kafka");

    private String type;
    private String description;
}
