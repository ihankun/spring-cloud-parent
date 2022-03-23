package com.hankun.parent.commons.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CommonErrorCode implements IErrorCode{

    /**
     * 上级不存在
     */
    UP_LEVEL_NOT_FOUND("0001", "上级不存在"),
    /**
     * 返回数据为空
     */
    RESULT_NULL("0002", "返回数据为空"),
    /**
     * 日期格式错误
     */
    DATE_FORMAT_ERROR("0003", "日期格式错误"),
    DOMAIN_NOT_CONFIG("0004", "域名信息暂未配置，请在bootstrap.properties中引入config-common-domain.properties后使用"),

    DOMAIN_NOT_FOUND("0005", "域名暂未配置"),

    USER_NOT_LOGIN("0006", "当前用户未登录"),


    ;
    private String code;
    private String msg;

    @Override
    public String prefix() {
        return "common";
    }
}
