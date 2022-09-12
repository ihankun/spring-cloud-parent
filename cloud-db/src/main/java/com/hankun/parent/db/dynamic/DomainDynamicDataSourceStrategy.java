package com.hankun.parent.db.dynamic;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.ds.ItemDataSource;
import com.baomidou.dynamic.datasource.strategy.DynamicDataSourceStrategy;
import com.hankun.parent.commons.context.DomainContext;
import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;
import com.hankun.parent.commons.utils.SpringHelpers;
import com.hankun.parent.db.exceptions.CommonDbException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DomainDynamicDataSourceStrategy implements DynamicDataSourceStrategy {

    private static final String DS_SPLIT = "_";
    /**
     * seta回滚线程
     */
    private static final String FIRST_THREAD_NAME = "main";


    private static final String DS_DOMAIN_REROUTE = "msun.ds.reroute.";
    private static final String DS_REROUTE = "msun.ds.db.domain.reroute.enable";


    /**
     * 分隔字符串
     */
    private static final String SPLIT = ",";


    @Override
    public DataSource determineDataSource(List<DataSource> dataSources) {
        LoginUserInfo loginUserInfo = LoginUserContext.getLoginUserInfo();
        String threadName = Thread.currentThread().getName();


        if (CollectionUtils.isEmpty(dataSources)) {
            String msg = "没有可用线程列表，请检查数据源是否配置正确，当前线程名称" + threadName + ",当前登录用户信息" + (loginUserInfo == null ? "null" : JSON.toJSONString(loginUserInfo)) + ",当前域名信息" + DomainContext.getCurrentDomain();
            log.error(msg);
            throw new RuntimeException(msg);
        }

        //域名不为空，则根据域名决定
        String domain = DomainContext.getCurrentDomain();
        if (!StringUtils.isEmpty(domain)) {
            ItemDataSource dataSource = (ItemDataSource) domainDataSource(domain, dataSources);
            log.info("DomainDynamicDataSourceStrategy.determineDataSource.success,threadName={},datasource={}", threadName, dataSource == null ? "根据域名筛选数据源为空" : dataSource.getName());
            return dataSource;
        }


        //域名为空，且符合默认线程，则取第一个返回
        if (startWith(FIRST_THREAD_NAME, threadName)) {
            ItemDataSource dataSource = (ItemDataSource) firstDataSource(dataSources);
            log.info("DomainDynamicDataSourceStrategy.determineDataSource.fail.first,threadName={},datasource={}", threadName, dataSource.getName());
            return dataSource;
        }

        //其他线程，返回错误
        String msg = "没有找到可选范围内的线程池，当前线程名称" + threadName + ",当前登录用户信息" + (loginUserInfo == null ? "null" : JSON.toJSONString(loginUserInfo)) + ",当前域名信息" + DomainContext.getCurrentDomain();
        log.error(msg);
        throw new RuntimeException(msg);
    }


    private boolean startWith(String standard, String threadName) {
        boolean anyMatch = Arrays.stream(standard.split(SPLIT)).anyMatch(s -> threadName.startsWith(s));
        return anyMatch;
    }


    /**
     * 查找第一个数据源
     *
     * @param dataSources
     * @return
     */
    private DataSource firstDataSource(List<DataSource> dataSources) {
        return dataSources.stream().findFirst().orElse(null);
    }

    /**
     * 根据域名获取线程
     *
     * @param dataSources 所有数据源
     * @return 对应的数据源
     */
    private DataSource domainDataSource(String domain, List<DataSource> dataSources) {

        ApplicationContext context = SpringHelpers.context();
        log.info("DomainDynamicDataSourceStrategy.domainDataSource.match.by.domain,domain={}", domain);
        //尝试重定向到其他域名
        Environment environment = context.getEnvironment();
        boolean activeReroute = Boolean.parseBoolean(environment.getProperty(DS_REROUTE));
        if (activeReroute) {
            String reroute = environment.getProperty(DS_DOMAIN_REROUTE + domain);
            if (reroute != null) {
                log.info("DomainDynamicDataSourceStrategy.domainDataSource.domain,reroute,domain={},reroute={}", domain, reroute);
                domain = reroute;
            }
        }
        //强制转换，需要将DataSource转换为ItemDataSource，否则无法获取其Name
        List<ItemDataSource> itemDataSourceList = dataSources.stream().map(item -> (ItemDataSource) item).collect(Collectors.toList());
        String name = itemDataSourceList.get(0).getName();
        String alias = name.contains(DS_SPLIT) ? name.split(DS_SPLIT)[0] : name;
        String dsName = alias + DS_SPLIT + domain;
        Optional<ItemDataSource> dataSource = itemDataSourceList.stream().filter(ds -> ds.getName().equals(dsName)).findFirst();
        if (dataSource.isPresent()) {
            return dataSource.get();
        }
        //未找到则尝试生成数据源
        log.info("DomainDynamicDataSourceStrategy.domainDataSource.createDs.domain,domain={},alias={}", domain, alias);
        DataSourceCacheCreator creator = context.getBean(DataSourceCacheCreator.class);
        //生成数据源
        DataSource source = creator.createDataSource(alias, dsName, domain);
        if (source == null) {
            log.error("HospitalDynamicDataSourceStrategy.determineDataSource.fail.create.ds.by.dataSource,domain={},dataSource={},itemDataSourceList={}",
                    domain, dsName, itemDataSourceList.stream().map(ItemDataSource::getName).toArray());
            throw new CommonDbException("无法找到匹配的数据源" + ",thread:" + Thread.currentThread().getName() + ",域名:" + domain + ",数据源:" + dsName);
        }
        return source;
    }
}
