package com.hankun.parent.cache.core;

import com.hankun.parent.cache.key.CacheKey;

import java.util.Map;

/**
 * @author hankun
 */
public interface MapCache<K, V> {

    //---------- 通用方法开始 ----------//

    /**
     * 保存
     *
     * @param key    缓存key
     * @param value  缓存value
     * @param expire 过期时间
     * @return
     */
    boolean save(CacheKey key, Map<K, V> value, Long expire);

    /**
     * 获取所有缓存数据
     *
     * @param key
     * @return
     */
    Map<K, V> get(CacheKey key);

    /**
     * 删除缓存
     *
     * @param key
     * @return
     */
    boolean del(CacheKey key);

    /**
     * 更新缓存
     *
     * @param key
     * @param value
     * @return
     */
    boolean update(CacheKey key, Map<K, V> value);

    /**
     * 修改过期时间
     *
     * @param key
     * @param expire
     * @return
     */
    boolean expire(CacheKey key, Long expire);

    /**
     * 缓存是否存在
     *
     * @param key
     * @return
     */
    boolean exits(CacheKey key);

    //---------- 通用方法结束 ----------//

    /**
     * 得到某个实体
     *
     * @param key
     * @param mapKey
     * @return
     */
    V getValue(CacheKey key, K mapKey);

    /**
     * 新增元素
     *
     * @param key
     * @param mapKey
     * @param value
     * @return
     */
    boolean put(CacheKey key, K mapKey, V value);

    /**
     * 移除元素
     *
     * @param key
     * @param mapKey
     * @return
     */
    boolean remove(CacheKey key, K mapKey);


    /**
     * 元素数量
     *
     * @param key
     * @return
     */
    Long size(CacheKey key);

    /**
     * 插入全部
     *
     * @param key
     * @param map
     * @return
     */
    boolean putAll(CacheKey key, Map<K, V> map);
}
