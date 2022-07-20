package com.hankun.parent.commons.api;


import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;

public interface BaseService {

    /**
     * 获取当前登陆人的所属机构id
     */
    default Long getOrgId() {
        LoginUserInfo loginUser = getLoginUser();
        return loginUser != null && loginUser.getOrgId() != null ? loginUser.getOrgId() : BasePO.DEFAULT_ID;
    }

    /**
     * 获取当前登录用户
     */
    default LoginUserInfo getLoginUser() {
        return LoginUserContext.getLoginUserInfo();
    }
}
