package com.hankun.parent.mq.producer;

import com.hankun.parent.mq.constants.KunMqSendResult;

/**
 * @author hankun
 */
public interface KunMqSendCallback {

    /**
     * 成功回调
     *
     * @param result
     */
    void success(KunMqSendResult result);

    /**
     * 异常回调
     *
     * @param e
     */
    void exception(Throwable e);
}
