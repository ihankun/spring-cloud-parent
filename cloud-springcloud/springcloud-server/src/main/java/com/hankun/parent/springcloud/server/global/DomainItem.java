package com.hankun.parent.springcloud.server.global;

import lombok.Data;

import java.util.Map;

@Data
public class DomainItem {

    /**
     * 访问域名
     */
    private String host;

    /**
     * 机构ID
     */
    private Long orgId;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 医院名称
     */
    private String name;

    /**
     * 顶层机构ID
     */
    private String msunOrgId;

    /**
     * 院内出口IP
     */
    private String localIp;

    /**
     * 业务IP，key=业务标识 value=Ip地址
     */
    private Map<String, String> businessIp;

    /**
     * 是否支持定时任务
     */
    private Boolean isSupportJob = true;
}
