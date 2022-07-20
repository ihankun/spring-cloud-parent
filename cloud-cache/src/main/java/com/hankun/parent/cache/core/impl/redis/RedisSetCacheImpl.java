package com.hankun.parent.cache.core.impl.redis;

import com.hankun.parent.cache.core.SetCache;
import com.hankun.parent.cache.key.CacheKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author hankun
 */
@Slf4j
public class RedisSetCacheImpl<V> extends AbstractRedisCache implements SetCache<V> {

    @Override
    public List<String> pop(CacheKey key, int size) {

        try {
            List<String> list = getRedisTemplate().opsForSet().pop(key.get(), size);
            return list;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean contain(CacheKey key, V value) {
        try {
            boolean contains = getRedisTemplate().opsForSet().members(key.get()).contains(value);
            return contains;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean put(CacheKey key, V value) {
        try {
            getRedisTemplate().opsForSet().add(key.get(), value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean putAll(CacheKey key, Set<V> values) {
        try {
            values.forEach(item -> {
                getRedisTemplate().opsForSet().add(key.get(), item);
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean remove(CacheKey key, V value) {
        try {
            getRedisTemplate().opsForSet().remove(key.get(), value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public Long size(CacheKey key) {
        try {
            Long size = getRedisTemplate().opsForSet().size(key.get());
            return size;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    public boolean save(CacheKey key, Set<V> value, Long expire) {
        try {

            String[] array = value.toArray(new String[0]);
            getRedisTemplate().opsForSet().add(key.get(), array);
            expire(key, expire);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public Set<V> get(CacheKey key) {
        try {
            Set<V> set = getRedisTemplate().opsForSet().members(key.get());
            return set;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
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
    public boolean update(CacheKey key, Set<V> value) {
        try {
            value.forEach(item -> getRedisTemplate().opsForSet().add(key.get(), item));
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
            Long size = getRedisTemplate().opsForSet().size(key.get());
            if (size == null || size == 0) {
                return false;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }
}
