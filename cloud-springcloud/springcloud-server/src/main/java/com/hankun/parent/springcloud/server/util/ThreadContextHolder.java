package com.hankun.parent.springcloud.server.util;

import com.hankun.parent.commons.context.*;
import com.hankun.parent.log.context.TraceLogContext;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadContextHolder {

    private LoginUserInfo loginUserInfo;
    private String domain;
    private String businessTime;
    private String gray;
    private String traceId;
    private String xid;

    /**
     * 捕获当前线程上下文信息
     *
     * @return
     */
    public static ThreadContextHolder capture() {
        ThreadContextHolder holder = new ThreadContextHolder();
        holder.loginUserInfo = LoginUserContext.getLoginUserInfo();
        holder.domain = DomainContext.getCurrentDomain();
        holder.businessTime = BusinessStartTimeContext.getTimeStr();
        holder.gray = GrayContext.get();
        holder.traceId = TraceLogContext.get();
        holder.xid = RootContext.getXID();
        return holder;
    }

    /**
     * 清理当前线程上下文内容
     */
    public static void clear() {
        LoginUserContext.clear();
        DomainContext.clear();
        BusinessStartTimeContext.clear();
        GrayContext.clear();
        TraceLogContext.reset();
    }

    /**
     * 注入到新线程上下文中
     */
    public void inject() {
        LoginUserContext.mock(this.loginUserInfo);
        DomainContext.mock(this.domain);
        BusinessStartTimeContext.mock(this.businessTime);
        GrayContext.mock(this.gray);
        TraceLogContext.set(this.traceId);
    }


    @Override
    public String toString() {
        return "KunThreadContextUtil{" +
                "loginUserInfo=" + loginUserInfo +
                ", domain='" + domain + '\'' +
                ", businessTime='" + businessTime + '\'' +
                ", gray='" + gray + '\'' +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
