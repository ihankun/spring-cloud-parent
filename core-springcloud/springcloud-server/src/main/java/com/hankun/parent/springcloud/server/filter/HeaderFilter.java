package com.hankun.parent.springcloud.server.filter;

import com.alibaba.fastjson.JSON;
import com.hankun.parent.commons.context.*;
import com.hankun.parent.commons.id.IdGenerator;
import com.hankun.parent.log.context.TraceLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

//@Configuration
@Slf4j
//@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class HeaderFilter implements WebMvcConfigurer {

    Base64.Decoder decoder = Base64.getDecoder();

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

                String url = request.getRequestURL().toString();

                //用户登录信息获取
                String loginUserInfoEncoder = request.getHeader(LoginUserContext.LOGIN_USER_KEY);
                if (!StringUtils.isEmpty(loginUserInfoEncoder)) {
                    String loginUserInfoJson = new String(decoder.decode(loginUserInfoEncoder.getBytes()));
                    LoginUserInfo userInfo = JSON.parseObject(loginUserInfoJson, LoginUserInfo.class);
                    LoginUserContext.mock(userInfo);
                    debug("HeaderFilter.userInfo.header.get,url={},userInfo={}", url, userInfo);
                } else {
                    debug("HeaderFilter.userInfo.header.get.null,url={}", url);
                }
                //域名信息获取
                String host = request.getHeader(DomainContext.DOMAIN_HEADER_NAME);
                if (!StringUtils.isEmpty(host)) {
                    host = new String(decoder.decode(host));
                    DomainContext.mock(host);
                    debug("HeaderFilter.host.header.get,url={},host={}", url, host);
                } else {
                    debug("HeaderFilter.host.header.get.null,url={}", url);
                }
                //获取业务发生时间
                String time = request.getHeader(BusinessStartTimeContext.BUSINESS_START_TIME_HEADER_NAME);
                if (!StringUtils.isEmpty(time)) {
                    time = new String(decoder.decode(time));
                    BusinessStartTimeContext.mock(time);
                    debug("HeaderFilter.time.header.get,url={},time={}", url, time);
                } else {
                    debug("HeaderFilter.time.header.get.null,url={}", url);
                }

                //获取traceId
                String traceId = request.getHeader(TraceLogContext.TRACE_HEADER_NAME);
                //如果获取不到traceId，则尝试主动生成一个ID
                if (StringUtils.isEmpty(traceId)) {
                    traceId = IdGenerator.ins().generator().toString();
                    debug("HeaderFilter.trace.header.init,url={},traceId={}", url, traceId);
                }
                TraceLogContext.set(traceId);
                debug("HeaderFilter.traceId.header.set,url={},traceId={}", url, traceId);


                //获取灰度标识
                String gray = request.getHeader(GrayContext.GRAY_HEADER_NAME);
                if (!StringUtils.isEmpty(gray)) {
                    GrayContext.mock(gray);
                    debug("HeaderFilter.gray.header.get,url={},gray={}", url, gray);
                } else {
                    debug("HeaderFilter.gray.header.get.null,url={}", url);
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                debug("HeaderFilter.Context.clear");
                LoginUserContext.clear();
                DomainContext.clear();
                TraceLogContext.reset();
                BusinessStartTimeContext.clear();
                GrayContext.clear();
            }
        });
    }

    private void debug(String msg, Object... param) {
        if (log.isDebugEnabled()) {
            log.debug(msg, param);
        }
    }
}
