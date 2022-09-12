package com.hankun.parent.requestlock.aspect;

import com.hankun.parent.cache.key.CacheKey;
import com.hankun.parent.cache.key.impl.OrgCacheKey;
import com.hankun.parent.cache.lock.LockCallback;
import com.hankun.parent.cache.lock.RedissonLock;
import com.hankun.parent.commons.api.BaseService;
import com.hankun.parent.commons.error.IErrorCode;
import com.hankun.parent.commons.exception.BusinessException;
import com.hankun.parent.requestlock.annotation.Lock;
import com.hankun.parent.springcloud.api.LockKey;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public class LockAspect implements BaseService, Ordered, PriorityOrdered {

    /**
     * 默认异常前缀
     */
    private static final String DEFAULT_PREFIX = "businessLock";

    /**
     * 默认错误编码
     */
    private static final String DEFAULT_CODE = "0002";

    /**
     * 缓存管理器
     */
    @Resource
    private RedissonLock redissonLock;

    @Around("@annotation(com.hankun.parent.requestlock.annotation.Lock)")
    public Object around(ProceedingJoinPoint point) {
        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        Method method = methodSignature.getMethod();
        Lock lock = method.getAnnotation(Lock.class);
        long waitTime = lock.waitTime();
        long leaseTime = lock.leaseTime();
        IErrorCode errorCode = getErrorCode(lock);
        return redissonLock.lock(getOrgCacheKey(point).get(), waitTime, leaseTime, TimeUnit.SECONDS, new LockCallback<Object>() {
            @Override
            public Object success() {
                try {
                    return point.proceed(point.getArgs());
                } catch (BusinessException e) {
                    throw e;
                } catch (Throwable throwable) {
                    if (throwable instanceof BusinessException) {
                        throw BusinessException.build(getExceptionErrorCode(throwable.getMessage()));
                    }
                    throw new RuntimeException(throwable);
                }
            }

            @Override
            public Object fail() {
                throw BusinessException.build(errorCode);
            }
        });
    }


    /**
     * 获取异常信息
     *
     * @param message
     * @return
     */
    private IErrorCode getExceptionErrorCode(String message) {
        return new IErrorCode() {
            @Override
            public String prefix() {
                return DEFAULT_PREFIX;
            }

            @Override
            public String getCode() {
                return DEFAULT_CODE;
            }

            @Override
            public String getMsg() {
                return message;
            }
        };
    }

    /**
     * 获取请求锁的key
     *
     * @param point
     * @return
     */
    private CacheKey getOrgCacheKey(ProceedingJoinPoint point) {
        LockKey lockKey = getLockKey(point);
        if (lockKey == null) {
            throw BusinessException.build(getExceptionErrorCode("未实现" + LockKey.class.getName() + "接口"));
        }
        if (StringUtils.isEmpty(lockKey.getLockKey())) {
            throw BusinessException.build(getExceptionErrorCode(LockKey.class.getName() + "接口，getLockKey()不能为空"));
        }
        return OrgCacheKey.build(DEFAULT_PREFIX).orgId(String.valueOf(getOrgId())).key(lockKey.getLockKey());
    }

    /**
     * 获取加锁key
     *
     * @param point
     * @return
     */
    private LockKey getLockKey(ProceedingJoinPoint point) {
        //获取参数对象
        Object[] args = point.getArgs();
        if (args == null || args.length == 0) {
            throw BusinessException.build(getExceptionErrorCode("无参方法不允许使用注解"));
        }
        for (Object arg : args) {
            if (arg instanceof LockKey) {
                return (LockKey) arg;
            }
        }
        return null;
    }

    /**
     * 根据注解信息生成ErrorCode信息
     *
     * @param lock
     * @return
     */
    private IErrorCode getErrorCode(Lock lock) {

        String value = lock.value();

        if (StringUtils.isEmpty(value)) {
            value = Lock.MESSAGE;
        }

        String finalValue = value;
        return new IErrorCode() {
            @Override
            public String prefix() {
                return DEFAULT_PREFIX;
            }

            @Override
            public String getCode() {
                return DEFAULT_CODE;
            }

            @Override
            public String getMsg() {
                return finalValue;
            }
        };
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
