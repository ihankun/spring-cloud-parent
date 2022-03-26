//package com.hankun.parent.springcloud.server.filter;
//
//import com.hankun.parent.commons.context.*;
//import com.hankun.parent.commons.id.IdGenerator;
//import com.hankun.parent.log.context.TraceLogContext;
//import feign.RequestInterceptor;
//import feign.RequestTemplate;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.util.StringUtils;
//
//import java.util.Base64;
//
////@Configuration
////@ConditionalOnClass(FeignClient.class)
//@Slf4j
//public class FeignFilter implements RequestInterceptor {
//
//    Base64.Encoder encoder = Base64.getEncoder();
//
//    @Override
//    public void apply(RequestTemplate template) {
//
//        String url = template.url();
//
//        //从上下文中取出用户信息，放置到Http请求Header中进行透传
//        LoginUserInfo userInfo = LoginUserContext.getLoginUserInfo();
//        if (userInfo != null) {
//            debug("FeignFilter.loginUser.header.set,url={},userInfo={}", url, userInfo);
//            String encode = new String(encoder.encode(userInfo.toJson().getBytes()));
//            template.header(LoginUserContext.LOGIN_USER_KEY, encode);
//        } else {
//            debug("FeignFilter.loginUser.header.null,url={}", url);
//        }
//
//        //从上下文中取出域名信息，放置到Http请求Header中进行透传
//        String domain = DomainContext.getCurrentDomain();
//        if (!StringUtils.isEmpty(domain)) {
//            debug("FeignFilter.domain.header.set,url={},domain={}", url, domain);
//            domain = encoder.encodeToString(domain.getBytes());
//            template.header(DomainContext.DOMAIN_HEADER_NAME, domain);
//        } else {
//            debug("FeignFilter.domain.header.null,url={}", url);
//        }
//
//        //从上下文中取出业务首次发生时间
//        String time = BusinessStartTimeContext.getTimeStr();
//        if (!StringUtils.isEmpty(time)) {
//            debug("FeignFilter.time.header.set,url={},time={}", url, time);
//            time = encoder.encodeToString(time.getBytes());
//            template.header(BusinessStartTimeContext.BUSINESS_START_TIME_HEADER_NAME, time);
//        } else {
//            debug("FeignFilter.time.header.null,url={}", url);
//        }
//
//        //从上下文中取出traceId
//        String traceId = TraceLogContext.get();
//        if (StringUtils.isEmpty(traceId)) {
//            traceId = IdGenerator.ins().generator().toString();
//            debug("FeignFilter.trace.header.init,url={},traceId={}", url, traceId);
//        }
//        debug("FeignFilter.trace.header.set,url={},traceId={}", url, traceId);
//        template.header(TraceLogContext.TRACE_HEADER_NAME, traceId);
//
//        //从上下文中取出灰度标识gray
//        String gray = GrayContext.get();
//        if (!StringUtils.isEmpty(gray)) {
//            debug("FeignFilter.gray.header.init,url={},gray={}", url, gray);
//            template.header(GrayContext.GRAY_HEADER_NAME, gray);
//        } else {
//            debug("FeignFilter.gray.header.set,url={},gray={}", url, gray);
//        }
//    }
//
//
//    private void debug(String msg, Object... param) {
//        if (log.isDebugEnabled()) {
//            log.debug(msg, param);
//        }
//    }
//}
