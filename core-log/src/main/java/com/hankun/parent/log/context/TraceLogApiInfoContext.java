package com.hankun.parent.log.context;

import org.springframework.core.NamedThreadLocal;
import springfox.documentation.service.ApiInfo;

/**
 * @author hankun
 */
public class TraceLogApiInfoContext {

    public static final String TRACE_HEADER_NAME = "traceApiInfo";

    private static final ThreadLocal<ApiInfo> INHERITABLE_CONTEXT_HOLDER = new NamedThreadLocal<>("trace-log-api-context");

    /**
     * 重置当前线程的数据信息
     */
    public static void reset() {
        INHERITABLE_CONTEXT_HOLDER.remove();
    }

    /**
     * 将traceId设置到线程上
     */
    public static void set(ApiInfo apiInfo) {
        INHERITABLE_CONTEXT_HOLDER.set(apiInfo);
    }

    /**
     * 从线程的上下文中获取traceId
     *
     * @return
     */
    public static ApiInfo get() {
        return INHERITABLE_CONTEXT_HOLDER.get();
    }
}
