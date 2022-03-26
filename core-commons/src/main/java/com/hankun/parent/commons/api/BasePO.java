package com.hankun.parent.commons.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hankun.parent.commons.optimistic.LockVersion;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 请求参数基类
 * @author hankun
 */
@Data
public class BasePO extends BaseEntity {

    /**
     * 机构id列名
     */
    public static final String ORG_ID = "org_id";

    /**
     * 默认id
     */
    public static final Long DEFAULT_ID = 0L;

    /**
     * 默认用户名称
     */
    private static final String DEFAULT_USER_NAME = "管理员";

    /**
     * 机构id
     */
    public Long orgId;

    /**
     * 创建人id
     */
    public Long sysCreaterId;

    /**
     * 创建人名称
     */
    public String sysCreaterName;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sysCreateTime;

    /**
     * 更新人id
     */
    public Long sysUpdaterId;

    /**
     * 更新人名称
     */
    public String sysUpdaterName;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sysUpdateTime;

    /**
     * 乐观锁
     */
    @ApiModelProperty("乐观锁标识(更新、逻辑删除时必传)")
    @LockVersion
    public Integer version;

}
