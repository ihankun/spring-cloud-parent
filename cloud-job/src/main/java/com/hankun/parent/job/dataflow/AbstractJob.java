package com.hankun.parent.job.dataflow;

import com.hankun.parent.commons.utils.ServerStateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * JOB扩展类
 * @author hankun
 */
@Slf4j
public abstract class AbstractJob {

    private boolean isBooleanValue(String grayStr) {
        return String.valueOf(Boolean.TRUE).equalsIgnoreCase(grayStr) || String.valueOf(Boolean.FALSE).equalsIgnoreCase(grayStr);
    }

    /**
     * 是否灰度节点
     *
     * @return
     */
    protected boolean isGray() {
        log.info("灰度标识：{}", ServerStateUtil.getGrayMark());
        //布尔值的话，返回
        if (isBooleanValue(ServerStateUtil.getGrayMark())) {
            return Boolean.parseBoolean(ServerStateUtil.getGrayMark());
        }

        //其他情况:如灰灰度，空值等，返回false
        return Boolean.FALSE;
    }
}
