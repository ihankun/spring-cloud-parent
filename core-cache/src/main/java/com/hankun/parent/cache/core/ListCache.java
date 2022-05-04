package com.hankun.parent.cache.core;


import com.hankun.parent.cache.key.CacheKey;

import java.util.List;

/**
 * @author hankun
 */
public interface ListCache<V> {

    //---------- 通用方法开始 ----------//

    /**
     * 保存
     *
     * @param key    缓存key
     * @param value  缓存value
     * @param expire 过期时间
     * @return
     */
    boolean save(CacheKey key, List<V> value, Long expire);

    /**
     * 获取所有缓存数据
     *
     * @param key
     * @return
     */
    List<V> get(CacheKey key);

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
    boolean update(CacheKey key, List<V> value);

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
     * 弹出所有元素
     *
     * @param key
     * @param size
     * @return
     */
    List<V> pop(CacheKey key, int size);

    /**
     * 追加元素
     *
     * @param key
     * @param value
     * @return
     */
    boolean add(CacheKey key, V value);

    /**
     * 移除元素
     *
     * @param key
     * @param value
     * @return
     */
    boolean remove(CacheKey key, V value);
}
