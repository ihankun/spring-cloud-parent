package com.hankun.parent.log.constant;

import com.hankun.parent.log.context.TraceLogContext;
import com.hankun.parent.commons.id.IdGenerator;
import org.apache.commons.lang3.StringUtils;

public interface TraceLogConstant {

    /**
     * tracceId
     */
    public static final String TRACE_ID = "traceId";

    /**
     * 判断当前线程是否绑定了traceId
     */
    public static boolean isBindTraceId() {
        return TraceLogContext.get() != null;
    }

    /**
     * 获取Mdc中的traceId
     */
    public static String getTraceId() {
        String traceId = TraceLogContext.get();
        if (StringUtils.isEmpty(traceId)) {
            traceId = String.valueOf(IdGenerator.ins().generator());
        }
        return traceId;
    }

    /**
     * 项Mdc中设置traceId
     */
    public static void setTraceId(String traceId) {
        TraceLogContext.set(traceId);
    }
}
