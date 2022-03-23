package com.hankun.parent.commons.api;

import com.alibaba.fastjson.JSON;
import com.hankun.parent.commons.error.BaseErrorCode;
import com.hankun.parent.commons.error.IErrorCode;
import com.hankun.parent.commons.exception.BusinessException;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * 通用返回对象基类
 * @author hankun
 */
@Slf4j
@Data
public class ResponseResult<T> implements Serializable {

    @ApiModelProperty("标记是否成功")
    private boolean success;

    @ApiModelProperty("是否将返回值包装为ResponseResult")
    private boolean decorate = Boolean.TRUE;

    @ApiModelProperty("错误码")
    private String code;

    @ApiModelProperty("错误信息")
    private String message;

    @ApiModelProperty("业务对象，继承自BaseEntity")
    private T data;

    @ApiModelProperty("跟踪ID")
    private String traceId;

    @ApiModelProperty("异常名称")
    private String exceptionName;

    private static final String REPLACE_STR = "$";
    private static final String CODE_SPLIT = "@";
    private static final String NO_PASS = "1";

    public boolean getSuccess() {
        return success;
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * 构建错误信息
     */
    private static <T> ResponseResult build(IErrorCode code, T data, String[] params) {
        ResponseResult result = new ResponseResult();
        result.setSuccess(code.getCode().equals(BaseErrorCode.SUCCESS.getCode()) ? Boolean.TRUE : Boolean.FALSE);
        if (StringUtils.isEmpty(code.prefix())) {
            result.setCode(code.getCode());
        } else {
            result.setCode(code.prefix() + CODE_SPLIT + code.getCode());
        }
        String msg = code.getMsg();
        //如果包含占位符
        if (msg.contains(REPLACE_STR) && params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                String param = params[i];
                msg = msg.replaceAll("\\$" + (i + 1), param);
            }
        }
        result.setMessage(msg);
        result.setData(data);
        return result;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    /**
     * 判断是否为业务异常
     *
     * @return
     */
    public boolean isBusinessException() {
        return BusinessException.class.getName().equals(getExceptionName());
    }

    /**
     * 重新构造消息
     *
     * @param msg
     * @return
     */
    public ResponseResult rebuildMsg(String msg) {
        this.setMessage(msg);
        return this;
    }

    public ResponseResult setException(Throwable exception) {
        this.exceptionName = exception.getClass().getName();
        return this;
    }

    /**
     * 根据返回对象获取errCode
     *
     * @return
     */
    public IErrorCode convert() {

        String[] split = this.getCode().split(CODE_SPLIT);
        String prefix = "";
        String code = "";
        int length = 1;
        if (split.length == length) {
            code = split[0];
            prefix = "";
        }
        length = 2;
        if (split.length == length) {
            code = split[0];
            prefix = split[1];
        }
        String message = this.getMessage();

        String finalPrefix = prefix;
        String finalCode = code;
        return new IErrorCode() {
            @Override
            public String prefix() {
                return finalPrefix;
            }

            @Override
            public String getCode() {
                return finalCode;
            }

            @Override
            public String getMsg() {
                return message;
            }
        };
    }

    /**
     * 获取成功的结果
     *
     * @return
     */
    public static <T> ResponseResult<T> success() {
        return build(BaseErrorCode.SUCCESS, null, null);
    }

    /**
     * 获取 成功结果
     *
     * @param data 需要返回的数据
     * @return
     */
    public static <T> ResponseResult<T> success(T data) {
        return build(BaseErrorCode.SUCCESS, data, null);
    }

    /**
     * 获取失败的结果
     *
     * @return
     */
    public static <T> ResponseResult<T> error() {
        return build(BaseErrorCode.SYSTEM_ERROR, null, null);
    }

    /**
     * 获取失败的结果
     *
     * @param code 业务code
     * @return
     */
    public static <T> ResponseResult<T> error(IErrorCode code, String... params) {
        return build(code, null, params);
    }


    /**
     * 获取失败的结果
     *
     * @param code 业务code
     * @return
     */
    public static <T> ResponseResult<T> error(T data, IErrorCode code, String... params) {
        return build(code, data, params);
    }


    /**
     * 返回熔断结果
     *
     * @param throwable
     * @return
     */
    public static <T> ResponseResult<T> fallback(Throwable throwable) {
        log.error("ResponseResult.fallback,e={}", throwable);
        return build(BaseErrorCode.FALLBACK, null, new String[]{throwable.getMessage()});
    }

    /**
     * 获取业务异常的ErrorCode
     *
     * @return
     */
    public IErrorCode convertErrorCode() {
        if (isBusinessException()) {
            String code = getCode();
            return new IErrorCode() {
                @Override
                public String prefix() {
                    return "BusinessExceptionErrorCode";
                }

                @Override
                public String getCode() {
                    return code;
                }

                @Override
                public String getMsg() {
                    return getMessage();
                }
            };
        }
        return convert();
    }
}
