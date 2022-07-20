package com.hankun.parent.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hankun.parent.commons.utils.SpringHelpers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

/**
 * @author hankun
 */
@Slf4j
public abstract class AbstractConsumer {

    private static final String CONSUMER_KEY = "mq:consume:";
    private static final int CONSUMER_TIME = 3;
    private static final TimeUnit CONSUMER_TIME_UNIT = TimeUnit.DAYS;


    private StringRedisTemplate getMqRedisHelper() {

        try {
            StringRedisTemplate template = SpringHelpers.context().getBean(StringRedisTemplate.class);
            return template;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * 标记此消息已经被消费,确认消费
     */

    protected void confirmConsumed(String messageId) {
        try {
            StringRedisTemplate helper = getMqRedisHelper();
            if (helper != null) {
                Boolean result = helper.opsForValue().setIfAbsent(CONSUMER_KEY + messageId,
                        String.valueOf(System.currentTimeMillis()), CONSUMER_TIME,
                        CONSUMER_TIME_UNIT);
                log.info("AbstractConsumer.confirmConsumed,messageId={},result={}", messageId, result);
                return;
            }
        } catch (Exception e) {
            log.warn("AbstractConsumer.confirmConsumed.redis not init,place check [msun.mq.redis.url] config");
        }
    }

    /**
     * 检查是否被消费
     *
     * @param messageId
     * @return
     */
    @SneakyThrows
    protected boolean checkConsumed(String messageId) {

        try {
            StringRedisTemplate helper = getMqRedisHelper();
            if (helper != null) {
                boolean exits = !StringUtils.isEmpty(helper.opsForValue().get(CONSUMER_KEY + messageId));
                log.info("AbstractConsumer.checkConsumed,messageId={},consumed={}", messageId, exits);
                return exits;
            }
        } catch (Exception e) {
            log.warn("AbstractConsumer.checkConsumed.redis not init,place check [msun.mq.redis.url] config");
        }
        return false;
    }


    /**
     * 重置消息的消费
     *
     * @param messageId
     */
    protected void resetConsumed(String messageId) {
        try {
            StringRedisTemplate helper = getMqRedisHelper();
            if (helper != null) {
                helper.delete(CONSUMER_KEY + messageId);
                log.info("AbstractConsumer.resetConsumed,messageId={}", messageId);
                return;
            }
        } catch (Exception e) {
            log.warn("AbstractConsumer.resetConsumed.redis not init,place check [msun.mq.redis.url] config");
        }
    }


    protected Object objectToClass(Object data, ConsumerListener listener) {

        try {
            Type type = null;
            Type[] genericInterfaces = listener.getClass().getGenericInterfaces();
            if (genericInterfaces != null && genericInterfaces.length > 0) {
                type = ((ParameterizedType) genericInterfaces[0]).getActualTypeArguments()[0];
            } else {
                type = ((ParameterizedType) listener.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            }
            Object result = JSON.parseObject(JSON.parse(JSON.toJSONString(data)).toString(), type);
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }


    /**
     * 获取自定义主键
     *
     * @param config
     * @param msgId
     * @param data
     * @return
     */
    protected String getIdempotentKey(ConsumerListenerConfig config, String msgId, Object data) {
        if (StringUtils.isEmpty(config.getIdempotentKey())) {
            return msgId;
        }

        try {
            JSONObject dataObj = (JSONObject) JSON.toJSON(data);
            return dataObj.getString(config.getIdempotentKey());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

    }
}
