package com.hankun.parent.commons.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;

@Slf4j
public class LoginUserContext {

    public static final String LOGIN_USER_KEY = "login-user";

    /**
     * 线程上下文
     */
    private static final ThreadLocal<LoginUserInfo> CONTEXT_HOLDER = new NamedThreadLocal<>(LOGIN_USER_KEY);


    /**
     * 获取登陆用户信息
     */
    public static LoginUserInfo getLoginUserInfo() {
        //从线程上下文中获取，默认请求进入后，会自动从header中进行尝试获取用户信息，并放入线程上下文，此处只用获取即可
        LoginUserInfo loginUserInfo = CONTEXT_HOLDER.get();
        return loginUserInfo;
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 模拟登录用户信息，测试使用
     */
    public static void mock(LoginUserInfo loginUserInfo) {
        //将用户信息放入线程上下文，在Feign拦截器中，会尝试从线程上下文获取到用户信息并放入请求header
        if (loginUserInfo != null) {
            CONTEXT_HOLDER.set(loginUserInfo);
        }
    }
}
