package com.hankun.parent.db.dynamic.aspect;

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.hankun.parent.db.dynamic.DataSourceCacheCreator;
import com.hankun.parent.db.dynamic.PropertiesHolder;
import com.hankun.parent.db.exceptions.CommonDbException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hankun
 */
@Aspect
@Component
@Slf4j
public class PropertyAspect {

    @Resource
    Environment environment;

    @Resource
    PropertiesHolder holder;

    private String[] hospitals;

    private static final String DEFAULT_DS_SUFFIX = "msun.ds.hospital.default";
    private static final String DEFAULT_HOSPITAL = "defaultHospital";
    private static final String SCAN_DS_SUFFIX = "msun.ds.hospital.scan";

    private static final String DS_SPLIT = "_";
    private static final String DS_DOMAIN_END = "/";
    private static final String DS_QUESTION = "?";

    public static final String DS_DATA_SPLIT = "@";
    private static final String DS_POINT = ".";

    private static final String DOMAIN_SPLIT = ",";


    private static final String DOMAIN_DS_NAME_PREFIX = "msun.domain.ds.name.";
    private static final String DATASOURCE_PREFIX = "msun.ds.datasource.";
    private static final String DS_CONFIG = "msun.datasource.config.properties";
    private static final String DS_URL_SUFFIX = "msun.datasource.url.suffix";

    public static final String DS_DRIVER = "org.postgresql.Driver";
    private static final String URL_PREFIX = "jdbc:postgresql://";
    private static final String URL_SUFFIX = "useUnicode=true&characterEncoding=utf8&useSSL=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai";

    @Pointcut("execution(public * com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties.getDatasource())")
    public void loadPoint() {

    }

    /**
     * 修改系统加载的配置信息
     *
     * @param result 系统加载的配置信息
     */
    @AfterReturning(returning = "result", value = "loadPoint()")
    public void afterGet(Map<String, DataSourceProperty> result) {
        boolean isPro = Boolean.parseBoolean(environment.getProperty(DS_CONFIG));
        //取默认后缀
        String defaultSuffix = environment.getProperty(DEFAULT_DS_SUFFIX);
        if (StringUtils.isEmpty(defaultSuffix)) {
            defaultSuffix = DEFAULT_HOSPITAL;
        }
        String domains = environment.getProperty(SCAN_DS_SUFFIX);
        if (!StringUtils.isEmpty(domains)) {
            hospitals = domains.split(DOMAIN_SPLIT);
        } else {
            hospitals = new String[]{};
        }

        if ((!checkSource(result)) || isPro) {
            log.info("PropertyAspect.afterGet.use.bootstrap,load config info={},force={}", result.entrySet(), isPro);
            Map<String, DataSourceProperty> propertyMap = getProperties(defaultSuffix);
            if (propertyMap.size() == 0) {
                log.error("PropertyAspect.afterGet.getProperties.no.find.any.config!");
            }
            holder.setPropertyMap(propertyMap);
            result.clear();
            for (Map.Entry<String, DataSourceProperty> entry : propertyMap.entrySet()) {
                String key = updateSource(defaultSuffix, entry.getKey(), entry.getValue());
                result.put(key, entry.getValue());
            }
            return;
        }
        Map<String, String> domainConverter = getConverterMap();
        //获取默认域名
        String defDomain = domainConverter.get(defaultSuffix);
        if (defDomain == null) {
            defDomain = defaultSuffix;
        }
        log.info("PropertyAspect.afterGet.use.config.files,load config count={},defDomain={}", result.size(), defDomain);
        holder.setPropertyMap(result);
        Map<String, DataSourceProperty> data = new HashMap<>(result.size());
        for (Map.Entry<String, DataSourceProperty> entry : result.entrySet()) {
            if (!entry.getKey().contains(DS_SPLIT)) {
                String key = updateSource(defDomain, entry.getKey() + DS_SPLIT + defDomain, entry.getValue());
                data.put(key, entry.getValue());
            } else {
                String suffix = entry.getKey().split(DS_SPLIT)[1];
                String domain = domainConverter.get(suffix);
                if (domain == null) {
                    domain = suffix;
                }
                String key = updateSource(domain, entry.getKey(), entry.getValue());
                data.put(key, entry.getValue());
            }
        }
        result.clear();
        result.putAll(data);
    }

    private boolean checkSource(Map<String, DataSourceProperty> result) {
        if (result.size() == 0) {
            return false;
        }
        String master = environment.getProperty(DataSourceCacheCreator.DS_PRIMARY);
        if (master == null) {
            master = DataSourceCacheCreator.DS_NAME_MASTER;
        }
        for (String key : result.keySet()) {
            String alias = key.split(DS_SPLIT)[0];
            if (master.equals(alias)) {
                return true;
            }
        }
        return false;
    }


    private String updateSource(String domain, String key, DataSourceProperty property) {
        String alias = key.split(DS_SPLIT)[0];
        //更新数据源信息
        if (DataSourceCacheCreator.buildDataSource(property, environment, domain, alias)) {
            String db = DataSourceCacheCreator.getDb(property.getUrl());
            log.info("PropertyAspect.afterGet.updateSource.update.success,dataSource={},updateTo={},db={},usedName={}",
                    key, alias + DS_SPLIT + domain, db, property.getUsername());
            return alias + DS_SPLIT + domain;
        } else {
            for (String hospital : hospitals) {
                log.info("PropertyAspect.afterGet.updateSource.update.retry,domain={},hospitals={}", hospital, hospitals);
                if (DataSourceCacheCreator.buildDataSource(property, environment, hospital, alias)) {
                    String db = DataSourceCacheCreator.getDb(property.getUrl());
                    log.info("PropertyAspect.afterGet.updateSource.update.success,dataSource={},updateTo={},db={},usedName={}",
                            key, alias + DS_SPLIT + hospital, db, property.getUsername());
                    return alias + DS_SPLIT + hospital;
                }
            }
            String db = DataSourceCacheCreator.getDb(property.getUrl());
            String addr = DataSourceCacheCreator.getAddr(property.getUrl());
            log.info("PropertyAspect.afterGet.updateSource.do.not.change,dataSource={},dbName={},dbUrl={}", key, db, addr);
        }
        boolean needCheck = !Boolean.parseBoolean(environment.getProperty("msun.datasource.disable.check.connect"));
        if (needCheck && !DataSourceCacheCreator.checkDataSource(property)) {
            throw new CommonDbException("数据源无法连接，数据源名称:" + alias + DS_SPLIT + domain + ",用户:" + property.getUsername() + ",url:" + property.getUrl());
        }
        return alias + DS_SPLIT + domain;
    }

    /**
     * @return 后缀到域名的映射
     */
    private Map<String, String> getConverterMap() {
        Map<String, String> domainConverter = new HashMap<>(4);
        if (environment instanceof StandardEnvironment) {
            StandardEnvironment standardEnvironment = (StandardEnvironment) environment;
            for (PropertySource source : standardEnvironment.getPropertySources()) {
                Object o = source.getSource();
                if (o instanceof Map) {
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) o).entrySet()) {
                        if (entry.getKey().startsWith(DOMAIN_DS_NAME_PREFIX)) {
                            String domain = entry.getKey().substring(DOMAIN_DS_NAME_PREFIX.length());
                            domainConverter.put(entry.getValue().toString(), domain);
                        }
                    }
                }
            }
        }
        return domainConverter;
    }


    /**
     * @return 获取数据源配置信息
     */
    private Map<String, DataSourceProperty> getProperties(String def) {
        Map<String, DataSourceProperty> propertyMap = new HashMap<>(4);
        String suffix = environment.getProperty(DS_URL_SUFFIX);
        if (suffix == null) {
            suffix = URL_SUFFIX;
        }
        suffix = DS_QUESTION + suffix;
        if (environment instanceof StandardEnvironment) {
            StandardEnvironment standardEnvironment = (StandardEnvironment) environment;
            for (PropertySource source : standardEnvironment.getPropertySources()) {
                Object o = source.getSource();
                if (o instanceof Map) {
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) o).entrySet()) {
                        if (entry.getKey().startsWith(DATASOURCE_PREFIX)) {
                            String alias = entry.getKey().substring(DATASOURCE_PREFIX.length());
                            String data = entry.getValue().toString();
                            int pointA = data.indexOf(DS_DATA_SPLIT);
                            int pointB = data.indexOf(DS_POINT);
                            if (pointA < 0 || pointB < 0) {
                                log.error("PropertyAspect.afterGet.getProperties.wrong.config,info={},value={}",
                                        entry.getKey(), entry.getValue());
                                continue;
                            }
                            String addr = data.substring(0, pointA);
                            String db = data.substring(pointA + 1, pointB);
                            String user = data.substring(pointB + 1);
                            if (StringUtils.isEmpty(alias) || StringUtils.isEmpty(addr) || StringUtils.isEmpty(db) || StringUtils.isEmpty(user)) {
                                log.error("PropertyAspect.afterGet.getProperties.config.error,info={},value={}",
                                        entry.getKey(), entry.getValue());
                                continue;
                            }
                            DataSourceProperty property = new DataSourceProperty();
                            property.setUrl(URL_PREFIX + addr + DS_DOMAIN_END + db + suffix);
                            property.setUsername(user);
                            property.setDriverClassName(DS_DRIVER);
                            propertyMap.put(alias + DS_SPLIT + def, property);
                        }
                    }
                }
            }
        }
        return propertyMap;
    }
}
