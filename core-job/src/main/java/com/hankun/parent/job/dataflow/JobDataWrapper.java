package com.hankun.parent.job.dataflow;

import com.hankun.parent.job.dataflow.domain.JobDomainItem;
import lombok.Data;

import java.io.Serializable;

/**
 * 定时任务待处理数据包装类
 * @author hankun
 */
@Data
public class JobDataWrapper<T> implements Serializable {

    /**
     * 待处理数据
     */
    private T data;

    /**
     * 域名信息
     */
    private JobDomainItem domain;
}
