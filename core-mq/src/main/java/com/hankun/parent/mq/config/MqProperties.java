package com.hankun.parent.mq.config;

import lombok.Data;

/**
 * @author hankun
 */
@Data
public class MqProperties {

    private String url;

    private String type;

    private Integer timeOut;

    private MqConsumerProperties consumer;

    private MqProducerProperties producer;

    private boolean activeShared = false;
}
