package com.hankun.parent.commons.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;
import com.hankun.parent.commons.optimistic.annotation.LockVersion;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Calendar;
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

    /**
     * 设置更新信息
     */
    public void update() {
        LoginUserInfo loginUser = getLoginUser();
        this.sysUpdaterId = loginUser.getUserSysId();
        this.sysUpdateTime = Calendar.getInstance().getTime();
    }


    /**
     * 获取当前登录用户
     *
     * @return
     */
    protected LoginUserInfo getLoginUser() {
        LoginUserInfo loginUserInfo = LoginUserContext.getLoginUserInfo();
        if (loginUserInfo == null) {
            loginUserInfo = new LoginUserInfo();
            loginUserInfo.setUserId(DEFAULT_ID);
            loginUserInfo.setUserName(DEFAULT_USER_NAME);
            loginUserInfo.setUserSysId(DEFAULT_ID);
            loginUserInfo.setOrgId(DEFAULT_ID);
        }
        return loginUserInfo;
    }


    /**
     * 初始化创建、更新信息
     */
    public void init() {
        LoginUserInfo loginUser = getLoginUser();
        Long userId = loginUser.getUserSysId();
        String userName = loginUser.getUserName();
        this.sysCreateTime = Calendar.getInstance().getTime();
        this.sysCreaterId = userId;
        this.sysCreaterName = userName;
        this.version = 0;
        update();
        autoSetOrgId();
    }

    /**
     * 自动设置机构id
     */
    private void autoSetOrgId() {
        LoginUserInfo loginUser = getLoginUser();
        if (getOrgId() == null) {
            if (loginUser.getOrgId() != null) {
                this.orgId = loginUser.getOrgId();
                return;
            }
            this.orgId = DEFAULT_ID;
        }
    }

}
