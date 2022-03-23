package com.hankun.parent.commons.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.StringUtils;

/**
 * @author hankun
 */
@Slf4j
public class GrayContext {

    public static final String GRAY_HEADER_NAME = "gray";

    /**
     * 线程上下文
     */
    private static final ThreadLocal<String> CONTEXT_HOLDER = new NamedThreadLocal<>(GRAY_HEADER_NAME);

    /**
     * 获取当前请求是否是灰度流量
     */
    public static String get() {
        String gray = CONTEXT_HOLDER.get();
        return gray;
    }


    /**
     * 模拟域名信息，测试使用
     */
    public static void mock(String gray) {
        if (!StringUtils.isEmpty(gray)) {
            CONTEXT_HOLDER.set(gray);
        }

    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
