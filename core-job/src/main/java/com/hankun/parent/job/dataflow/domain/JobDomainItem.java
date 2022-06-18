package com.hankun.parent.job.dataflow.domain;

import lombok.Data;

/**
 * @author hankun
 */
@Data
public class JobDomainItem {

    /**
     * 访问域名
     */
    private String host;

    /**
     * 机构ID
     */
    private Long orgId;

    /**
     * 出口IP
     */
    private String localIp;

    /**
     * 是否支持Job
     */
    private Boolean isSupportJob = true;
}
