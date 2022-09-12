package com.hankun.parent.mq.rocketmq.consumer.strategy;

import com.hankun.parent.mq.config.MqProperties;
import org.apache.rocketmq.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.client.log.ClientLogger;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.logging.InternalLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hankun
 */
public class KunAllocateMessageQueueStrategy implements AllocateMessageQueueStrategy {

    private final InternalLogger log = ClientLogger.getLog();
    private static final String GRAY_MARK = "gray";
    private static final String PROD_MARK = "prod";
    private MqProperties mqProperties;

    public KunAllocateMessageQueueStrategy(MqProperties mqProperties) {
        this.mqProperties = mqProperties;
    }

    @Override
    public List<MessageQueue> allocate(String consumerGroup, String currentCID, List<MessageQueue> mqAll, List<String> cidAll) {
        if (currentCID == null || currentCID.length() < 1) {
            throw new IllegalArgumentException("currentCID is empty");
        }
        if (mqAll == null || mqAll.isEmpty()) {
            throw new IllegalArgumentException("mqAll is null or mqAll empty");
        }
        if (cidAll == null || cidAll.isEmpty()) {
            throw new IllegalArgumentException("cidAll is null or cidAll empty");
        }

        List<MessageQueue> selectedMqAll = mqAll;
        List<String> selectedCidAll = cidAll;
        if (currentCID.startsWith(GRAY_MARK)) {
            // 判断是否有生产节点是否有标识，无生产标识说明生产消费者未上线，走原有逻辑。有标识的生产者说明生产逻辑已上线走新逻辑
            // 生产节点未上线灰度节点上线的场景下会发生灰度节点注册消费者到灰度队列不成功的场景
            boolean isProdCidExisted = this.isProdCidExisted(cidAll);
            if (isProdCidExisted) {
                // 灰度消费节点消费灰度的消费队列
                selectedCidAll = cidAll.stream().filter(cid -> cid.startsWith(GRAY_MARK)).collect(Collectors.toList());
                selectedMqAll = mqAll.subList(0, mqProperties.getGraySize());
            }
        } else if (currentCID.startsWith(PROD_MARK)) {
            // 非灰度消费接节点首先判断是否有灰度消费节点，无灰度消费节点的场景消费灰度节点队列和非灰度节点队列
            // 1、判断当前topic是否存在其他的灰度消费者
            boolean isGrayCidExisted = this.isGrayCidExisted(cidAll);
            if (isGrayCidExisted) {
                // 有其他灰度消费者的场景下，当前非灰度消费节点仅消费非灰度的queue
                selectedCidAll = cidAll.stream().filter(cid -> cid.startsWith(PROD_MARK)).collect(Collectors.toList());
                selectedMqAll = mqAll.subList(mqProperties.getGraySize(), mqAll.size());
            }
            // 没有其他灰度消费者的场景下，当前非灰度消费节点也消费灰度的队列
        } else {
            // 灰灰度节点不进行消息消费
            return null;
        }

        List<MessageQueue> result = new ArrayList<MessageQueue>();
        if (!selectedCidAll.contains(currentCID)) {
            log.info("[BUG] ConsumerGroup: {} The consumerId: {} not in selectedCidAll: {}",
                    consumerGroup,
                    currentCID,
                    selectedCidAll);
            return result;
        }

        int index = selectedCidAll.indexOf(currentCID);
        int mod = selectedMqAll.size() % selectedCidAll.size();
        int averageSize =
                selectedMqAll.size() <= selectedCidAll.size() ? 1 : (mod > 0 && index < mod ? selectedMqAll.size() / selectedCidAll.size()
                        + 1 : selectedMqAll.size() / selectedCidAll.size());
        int startIndex = (mod > 0 && index < mod) ? index * averageSize : index * averageSize + mod;
        int range = Math.min(averageSize, selectedMqAll.size() - startIndex);
        for (int i = 0; i < range; i++) {
            result.add(selectedMqAll.get((startIndex + i) % selectedMqAll.size()));
        }
        return result;
    }

    @Override
    public String getName() {
        return "MSUN";
    }

    /**
     * 判断是否存在灰度消费节点
     * @return
     */
    private boolean isGrayCidExisted(List<String> cidAll) {
        boolean isGrayCidExisted = false;
        for (String cid : cidAll) {
            if (cid.startsWith(GRAY_MARK)) {
                isGrayCidExisted = true;
                break;
            }
        }
        return isGrayCidExisted;
    }

    /**
     * 判断是否存在生产消费节点
     * @return
     */
    private boolean isProdCidExisted(List<String> cidAll) {
        boolean isProdCidExisted = false;
        for (String cid : cidAll) {
            if (cid.startsWith(PROD_MARK)) {
                isProdCidExisted = true;
                break;
            }
        }
        return isProdCidExisted;
    }
}
