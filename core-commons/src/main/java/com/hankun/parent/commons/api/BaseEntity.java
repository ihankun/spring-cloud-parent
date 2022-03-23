package com.hankun.parent.commons.api;

import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson.JSON;

import java.io.Serializable;

/**
 * 对象基类 可序列化对象
 * @author hankun
 */
@Slf4j
public class BaseEntity implements Serializable {

    public String toJson(){
        return JSON.toJSONString(this);
    }
}
