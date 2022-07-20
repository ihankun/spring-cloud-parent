package com.hankun.parent.cache;

import com.hankun.parent.cache.core.ListCache;
import com.hankun.parent.cache.core.MapCache;
import com.hankun.parent.cache.core.SetCache;
import com.hankun.parent.cache.core.StringCache;

/**
 * cache注册
 * @author hankun
 */
public class CacheManager<K, V> {

    private StringCache stringCache;
    private MapCache<K, V> mapCache;
    private ListCache<V> listCache;
    private SetCache<V> setCache;

    public CacheManager(StringCache stringCache, MapCache mapCache, ListCache listCache, SetCache setCache) {
        this.stringCache = stringCache;
        this.mapCache = mapCache;
        this.listCache = listCache;
        this.setCache = setCache;
    }

    public StringCache string() {
        return stringCache;
    }

    public MapCache<K, V> map() {
        return mapCache;
    }

    public SetCache<V> set() {
        return setCache;
    }

    public ListCache<V> list() {
        return listCache;
    }
}
