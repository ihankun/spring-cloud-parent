package com.hankun.parent.log.context;

import org.springframework.core.NamedThreadLocal;

public class TraceLogContext {

    /**
     * 日志类型
     */
    public static final String LOG_TYPE = "logType";

    public static final String TRACE_HEADER_NAME = "traceId";

    private static final ThreadLocal<String> INHERITABLE_CONTEXT_HOLDER = new NamedThreadLocal<>("trace-log-context");

    /**
     * 重置当前线程的数据信息
     */
    public static void reset() {
        INHERITABLE_CONTEXT_HOLDER.remove();
    }

    /**
     * 将traceId设置到线程上
     */
    public static void set(String traceId) {
        INHERITABLE_CONTEXT_HOLDER.set(traceId);
    }

    /**
     * 从线程的上下文中获取traceId
     */
    public static String get() {
        return INHERITABLE_CONTEXT_HOLDER.get();
    }
}
