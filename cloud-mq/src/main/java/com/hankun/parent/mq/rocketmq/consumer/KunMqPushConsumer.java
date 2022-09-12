package com.hankun.parent.mq.rocketmq.consumer;

import org.apache.rocketmq.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.remoting.RPCHook;

public class KunMqPushConsumer extends DefaultMQPushConsumer {

    private static final String GRAY_MARK = "gray";
    private static final String PROD_MARK = "prod";
    private static final String CLIENT_ID_SPLIT = "@";
    private static String grayMark = "false";

    public KunMqPushConsumer(final String consumerGroup, RPCHook rpcHook,
                              AllocateMessageQueueStrategy allocateMessageQueueStrategy, String grayMark) {
        super(consumerGroup, rpcHook, allocateMessageQueueStrategy);
        this.grayMark = grayMark;
    }

    /**
     * 重写消费者ClientId的生成方式，用于区分生产和灰度节点的消费者
     * @return
     */
    @Override
    public String buildMQClientId() {
        StringBuilder sb = new StringBuilder();
        if (Boolean.FALSE.toString().equalsIgnoreCase(grayMark)) {
            // 生产
            sb.append(PROD_MARK);
            sb.append(CLIENT_ID_SPLIT);
        } else if (Boolean.TRUE.toString().equalsIgnoreCase(grayMark)){
            // 灰度
            sb.append(GRAY_MARK);
            sb.append(CLIENT_ID_SPLIT);
        }
        sb.append(this.getClientIP());
        sb.append(CLIENT_ID_SPLIT);
        sb.append(this.getInstanceName());
        if (!UtilAll.isBlank(this.getUnitName())) {
            sb.append(CLIENT_ID_SPLIT);
            sb.append(this.getUnitName());
        }

        return sb.toString();
    }
}
