package com.hankun.parent.db.sql;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.hankun.parent.db.config.KunDbConfig;
import com.hankun.parent.db.exceptions.CommonDbException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.sql.Connection;
import java.util.Properties;

/**
 * sql更新限制
 * @author hankun
 */
@Component
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class SqlUpdateInterceptor implements Interceptor {

    public static final String UPDATE_TIME = "sys_update_time";

    @Resource
    private KunDbConfig config;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!config.isCheckSql()) {
            log.info("SqlUpdateInterceptor.intercept.does.not.check.sql!");
            return invocation.proceed();
        }
        SqlCommandType type;
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        try {
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
            MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
            type = mappedStatement.getSqlCommandType();
        } catch (Throwable e) {
            log.error("SqlUpdateInterceptor.intercept.get.info.fail,e=!", e);
            return invocation.proceed();
        }
        if (SqlCommandType.SELECT.equals(type)) {
            return invocation.proceed();
        }
        BoundSql boundSql = statementHandler.getBoundSql();
        if (boundSql == null) {
            log.error("SqlUpdateInterceptor.intercept.get.boundSql.fail!");
            return invocation.proceed();
        }
        String sql = boundSql.getSql();
        if (StringUtils.isEmpty(sql)) {
            log.error("SqlUpdateInterceptor.intercept.get.sql.fail!");
            return invocation.proceed();
        }
        String info = sql.toLowerCase();
        if (SqlCommandType.INSERT.equals(type)) {
            int re = info.indexOf(UPDATE_TIME);
            if (re < 0) {
                log.error("SqlUpdateInterceptor.intercept.update.time.not.exist,sql={}", sql);
                throw new CommonDbException("insert操作未包含更新时间!");
            }
        } else if (SqlCommandType.UPDATE.equals(type)) {
            int re = info.indexOf(UPDATE_TIME);
            if (re < 0) {
                log.error("SqlUpdateInterceptor.intercept.update.time.not.exist,sql={}", sql);
                throw new CommonDbException("update操作未包含更新时间!");
            }
        } else {
            log.info("SqlUpdateInterceptor.intercept.skip.sql!");
            return invocation.proceed();
        }
        log.info("SqlUpdateInterceptor.intercept.check.sql.success!");
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}
