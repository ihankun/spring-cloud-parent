package com.hankun.parent.cache.core.impl.redis;

import com.hankun.parent.cache.core.StringCache;
import com.hankun.parent.cache.key.CacheKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author hankun
 */
@Slf4j
public class RedisStringCacheImpl extends AbstractRedisCache implements StringCache {

    @Override
    public boolean save(CacheKey key, String value, Long expire) {
        return save(key, value, expire, TimeUnit.SECONDS);
    }

    @Override
    public boolean setIfAbsent(CacheKey key, String value) {
        return getRedisTemplate().opsForValue().setIfAbsent(key.get(), value);
    }

    @Override
    public boolean setIfAbsent(CacheKey key, String value, long timeout, TimeUnit unit) {
        return getRedisTemplate().opsForValue().setIfAbsent(key.get(), value, timeout, unit);
    }

    /**
     * 保存
     *
     * @param key      缓存key
     * @param value    缓存value
     * @param expire   过期时间
     * @param timeUnit 过期时间单位
     * @return 是否设置成功
     */
    @Override
    public boolean save(CacheKey key, String value, Long expire, TimeUnit timeUnit) {
        try {
            getRedisTemplate().opsForValue().set(key.get(), value, expire, timeUnit);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public String get(CacheKey key) {
        String result = (String) getRedisTemplate().opsForValue().get(key.get());
        return result;
    }

    @Override
    public boolean del(CacheKey key) {
        try {
            getRedisTemplate().delete(key.get());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean update(CacheKey key, String value) {
        try {
            getRedisTemplate().opsForValue().set(key.get(), value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean expire(CacheKey key, Long expire) {
        try {
            getRedisTemplate().expire(key.get(), expire, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean exits(CacheKey key) {
        try {
            String value = (String) getRedisTemplate().opsForValue().get(key.get());
            if (StringUtils.isEmpty(value)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Long atomic(CacheKey key, Long num) {
        try {

            if (num == 0) {
                return null;
            }

            if (num > 0) {
                Long increment = getRedisTemplate().opsForValue().increment(key.get(), num);
                return increment;
            }


            Long decrement = getRedisTemplate().opsForValue().decrement(key.get(), Math.abs(num));
            return decrement;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

    }
}
