package com.hankun.parent.commons.error;

public interface IErrorCode {

    /**
     * 设置前缀
     */
    String prefix();

    /**
     * 获取Code
     */
    String getCode();

    /**
     * 获取错误信息
     */
    String getMsg();
}
