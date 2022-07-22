package com.hankun.parent.nosql.mongodb.handler;

/**
 * mongo游标执行器
 * @author hankun
 */
public interface Executor<T> {

    /**
     * 执行
     * @param cModel
     * @throws Exception
     */
    void invoke(T cModel) throws Exception;
}
