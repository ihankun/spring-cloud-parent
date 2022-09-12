package com.hankun.parent.commons.api;

import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson.JSON;

import java.io.Serializable;

/**
 * 对象基类 可序列化对象
 * @author hankun
 */
@Slf4j
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 3441090872456352378L;

    public String toJson(){
        return JSON.toJSONString(this);
    }

    /**
     * 从当前登陆上下文中获取机构id
     */
    public Long getOrgId() {
        try {
            LoginUserInfo loginUserInfo = LoginUserContext.getLoginUserInfo();
            return (loginUserInfo != null && loginUserInfo.getOrgId() != null) ? loginUserInfo.getOrgId() : BasePO.DEFAULT_ID;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
