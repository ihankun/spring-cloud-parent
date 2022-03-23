package com.hankun.parent.commons.remote;

import com.hankun.parent.commons.api.ResponseResult;
import com.hankun.parent.commons.error.CommonErrorCode;
import com.hankun.parent.commons.exception.BusinessException;

public class RemoteService {

    /**
     * 调用函数
     *
     * @param result   远程接口的返回结果
     * @param callBack 回调接口
     * @return ResponseResult的数据
     */
    public static final <T> T invoke(ResponseResult<T> result, RemoteCallBack<T> callBack) {
        if (result == null) {
            callBack.onFailure(BusinessException.build(CommonErrorCode.RESULT_NULL));
            return null;
        }
        if (!result.isSuccess()) {
            callBack.onFailure(BusinessException.build(result.convert()));
            return null;
        }
        callBack.onSuccess(result.getData());
        return result.getData();
    }
}
