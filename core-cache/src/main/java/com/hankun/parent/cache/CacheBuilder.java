package com.hankun.parent.cache;

import com.hankun.parent.cache.core.impl.ListCacheImpl;
import com.hankun.parent.cache.core.impl.MapCacheImpl;
import com.hankun.parent.cache.core.impl.SetCacheImpl;
import com.hankun.parent.cache.core.impl.StringCacheImpl;
import com.hankun.parent.cache.enums.CacheType;

/**
 * @author hankun
 */
public class CacheBuilder {

    public static CacheManager build(CacheType type) {
        CacheManager manager = null;
        if (type.equals(CacheType.REDIS)) {
            manager = new CacheManager(new StringCacheImpl(), new MapCacheImpl(), new ListCacheImpl(), new SetCacheImpl());
        }
        return manager;
    }
}
