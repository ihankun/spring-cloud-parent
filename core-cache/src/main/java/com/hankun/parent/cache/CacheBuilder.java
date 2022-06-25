package com.hankun.parent.cache;

import com.hankun.parent.cache.core.impl.redis.RedisListCacheImpl;
import com.hankun.parent.cache.core.impl.redis.RedisMapCacheImpl;
import com.hankun.parent.cache.core.impl.redis.RedisSetCacheImpl;
import com.hankun.parent.cache.core.impl.redis.RedisStringCacheImpl;
import com.hankun.parent.cache.enums.CacheType;

/**
 * @author hankun
 */
public class CacheBuilder {

    /**
     * 构造不同类型的cache管理器
     * @param type 类型
     * @return CacheManager
     */
    public static CacheManager build(CacheType type) {
        CacheManager manager = null;
        if (type.equals(CacheType.REDIS)) {
            manager = new CacheManager(new RedisStringCacheImpl(), new RedisMapCacheImpl(), new RedisListCacheImpl(), new RedisSetCacheImpl());
        }
        return manager;
    }
}
