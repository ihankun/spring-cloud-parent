package com.hankun.parent.cache.core.impl;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import com.hankun.parent.commons.utils.SpringHelpers;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author hankun
 */
public class AbstractRedisCache {

    protected RedisTemplate getRedisTemplate() {

        RedisTemplate template = SpringHelpers.context().getBean("redisTemplate", RedisTemplate.class);
        FastJsonRedisSerializer<Object> serializer = new FastJsonRedisSerializer<>(Object.class);

        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}
