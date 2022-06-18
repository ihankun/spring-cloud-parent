package com.hankun.parent.db.sql;

import com.hankun.parent.db.config.KunDbConfig;
import com.hankun.parent.db.exceptions.CommonDbException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.Statement;
import java.util.Collection;
import java.util.Properties;

/**
 * sql行数限制
 * @author hankun
 */
@Component
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class})
})
public class SqlLimitInterceptor implements Interceptor {

    @Resource
    private KunDbConfig config;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] datas = invocation.getArgs();
        if (datas.length < 1 || (!(datas[0] instanceof Statement))) {
            return invocation.proceed();
        }
        Statement statement = (Statement) datas[0];

        int rows = statement.getMaxRows();
        int maxRows = config.getMaxRows();
        if (rows == 0 || rows > maxRows) {
            statement.setMaxRows(maxRows);
            log.info("SqlLimitInterceptor.intercept.set.limit,limit={},originLimit={}", maxRows, rows);
        } else {
            log.info("SqlLimitInterceptor.intercept.limit.exists,originLimit={}", rows);
        }
        Object result = invocation.proceed();
        if (result instanceof Collection) {
            Collection<?> res = (Collection<?>) result;
            if (res.size() >= maxRows) {
                String sql = null;
                if (invocation.getTarget() instanceof RoutingStatementHandler) {
                    RoutingStatementHandler routingStatementHandler = (RoutingStatementHandler) invocation.getTarget();
                    sql = routingStatementHandler.getBoundSql().getSql();
                }
                log.error("sql查询结果行数超出限制,当前限制为{}，请检查sql!sql={}", maxRows, sql);
                throw new CommonDbException("sql查询结果行数超出限制!");
            }
        }
        return result;
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
