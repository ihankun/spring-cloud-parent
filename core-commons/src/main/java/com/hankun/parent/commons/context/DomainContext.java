package com.hankun.parent.commons.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.StringUtils;

/**
 * 记录当前请求的访问域名，绑定到线程中
 * @author hankun
 */
@Slf4j
public class DomainContext {

    public static final String DOMAIN_HEADER_NAME = "domain";

    /**
     * 线程上下文
     */
    private static final ThreadLocal<String> CONTEXT_HOLDER = new NamedThreadLocal<>(DOMAIN_HEADER_NAME);

    /**
     * 获取当前请求的访问域名
     */
    public static String getCurrentDomain() {
        String domain = CONTEXT_HOLDER.get();
        return domain;
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
