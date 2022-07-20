package com.hankun.parent.nosql.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author hankun
 */
@Component
public abstract class AbstractRedisService {

    @Resource
    StringRedisTemplate stringRedisTemplate;


    public static final String SPLIT = ":";

    /**
     * 指定项目名，用于Redis数据隔离
     *
     * @return
     */
    public abstract String projectName();

    private String prefix(String key) {
        StringBuilder builder = new StringBuilder(projectName());
        builder.append(SPLIT).append(key);
        return builder.toString();
    }


    /** ----------------------------------- 字符串操作----------------------------------- */

    /**
     * 设置key-value存储
     *
     * @param key
     * @param value
     * @param expireTime 秒
     */
    protected void stringSet(String key, String value, int expireTime) {
        stringRedisTemplate.opsForValue().set(prefix(key), value, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 字符串获取
     *
     * @param key
     * @return
     */
    protected String stringGet(String key) {
        return stringRedisTemplate.opsForValue().get(prefix(key));
    }

    /* ----------------------------------- Map操作----------------------------------- */


    /**
     * Map首次存入
     *
     * @param mapKey
     * @param value
     * @param expireTime
     */
    protected void mapSet(String mapKey, Map value, int expireTime) {
        stringRedisTemplate.opsForHash().putAll(prefix(mapKey), value);
        stringRedisTemplate.expire(prefix(mapKey), expireTime, TimeUnit.SECONDS);
    }

    /**
     * Map读取
     *
     * @param key
     * @return
     */
    protected Map mapGet(String key) {
        Map map = stringRedisTemplate.opsForHash().entries(prefix(key));
        return map;
    }

    /**
     * Map单条Entry写入
     *
     * @param mapKey
     * @param entryKey
     * @param entryValue
     */
    protected void mapPut(String mapKey, String entryKey, String entryValue) {
        stringRedisTemplate.opsForHash().put(prefix(mapKey), entryKey, entryValue);
    }

    /**
     * Map多条Entry写入
     *
     * @param mapKey
     * @param map
     */
    protected void mapPutAll(String mapKey, Map map) {
        stringRedisTemplate.opsForHash().putAll(prefix(mapKey), map);
    }

    /**
     * 移除多个Entry
     *
     * @param mapKey
     * @param entryKeys
     */
    protected void mapRemove(String mapKey, String... entryKeys) {
        stringRedisTemplate.opsForHash().delete(mapKey, entryKeys);
    }

    /* ----------------------------------- List操作----------------------------------- */

    /**
     * List首次写入
     *
     * @param key
     * @param list
     * @param expireTime
     */
    protected void listSet(String key, List list, int expireTime) {
        stringRedisTemplate.opsForList().leftPushAll(prefix(key), list);
        stringRedisTemplate.expire(prefix(key), expireTime, TimeUnit.SECONDS);
    }

    /**
     * List追加
     *
     * @param key
     * @param value
     */
    protected void listAdd(String key, String value) {
        stringRedisTemplate.opsForList().leftPush(prefix(key), value);
    }

    /**
     * List元素移除
     *
     * @param key
     * @param values
     */
    protected void listRemove(String key, String... values) {
        for (String value : values) {
            stringRedisTemplate.opsForList().remove(prefix(key), 1, value);
        }
    }

    /* ----------------------------------- Set操作----------------------------------- */


    /* ----------------------------------- 原子操作----------------------------------- */

    /**
     * 原子自增操作
     *
     * @param key
     * @return
     */
    protected Long increment(String key) {
        Long increment = stringRedisTemplate.opsForValue().increment(prefix(key));
        return increment;
    }

    /**
     * 原子自减操作
     *
     * @param key
     * @return
     */
    protected Long decrement(String key) {
        Long decrement = stringRedisTemplate.opsForValue().decrement(prefix(key));
        return decrement;
    }

}
