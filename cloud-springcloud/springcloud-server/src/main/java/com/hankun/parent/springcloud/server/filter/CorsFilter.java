//package com.hankun.parent.springcloud.server.filter;
//
//import lombok.Getter;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.util.StringUtils;
//
//import javax.servlet.*;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//
//@Slf4j
////@Configuration
////@RefreshScope
////@ConfigurationProperties(prefix = "kun")
//public class CorsFilter implements Filter {
//
//    private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
//    private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
//    private static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
//    private static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
//
//    @Setter
//    @Getter
//    private boolean cors = false;
//
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
//
//        if (!cors) {
//            chain.doFilter(servletRequest, servletResponse);
//            return;
//        }
//
//        HttpServletRequest request = (HttpServletRequest) servletRequest;
//        String origin = request.getHeader("Origin");
//        if (log.isDebugEnabled()) {
//            log.debug("CorsFilter.doFilter.start,origin={}", origin);
//        }
//
//        HttpServletResponse res = (HttpServletResponse) servletResponse;
//        if (StringUtils.isEmpty(res.getHeader(ALLOW_CREDENTIALS))) {
//            res.addHeader(ALLOW_CREDENTIALS, "true");
//        }
//        if (StringUtils.isEmpty(res.getHeader(ALLOW_ORIGIN))) {
//            res.addHeader(ALLOW_ORIGIN, origin);
//        }
//        if (StringUtils.isEmpty(res.getHeader(ALLOW_METHODS))) {
//            res.addHeader(ALLOW_METHODS, "*");
//        }
//        if (StringUtils.isEmpty(res.getHeader(ALLOW_HEADERS))) {
//            String str = "Login-User,Authorization,DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With," +
//                    "If-Modified-Since," +
//                    "Cache-Control,Content-Type";
//            res.addHeader(ALLOW_HEADERS, str + "," + str.toLowerCase());
//        }
//
//        chain.doFilter(servletRequest, servletResponse);
//    }
//}
