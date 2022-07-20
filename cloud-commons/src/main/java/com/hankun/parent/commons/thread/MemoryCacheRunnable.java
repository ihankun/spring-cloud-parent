package com.hankun.parent.commons.thread;

import org.springframework.stereotype.Component;

/**
 * 共享内存线程
 * @author hankun
 */
@Component
public interface MemoryCacheRunnable {

    /**
     * 是否独立线程运行 默认共享线程
     *
     * @return
     */
    boolean standalone();

    /**
     * 是否开启 默认关闭
     *
     * @return
     */
    boolean open();

    /**
     * 间隔运行时间 秒
     *
     * @return
     */
    int interval();
}
