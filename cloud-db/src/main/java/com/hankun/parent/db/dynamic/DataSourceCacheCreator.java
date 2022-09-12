package com.hankun.parent.db.dynamic;

import cn.hutool.crypto.digest.MD5;
import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidConfig;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidSlf4jConfig;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidStatConfig;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidWallConfig;
import com.hankun.parent.commons.utils.SpringHelpers;
import com.hankun.parent.db.dynamic.bean.DataSourceBuildProperty;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class DataSourceCacheCreator implements ApplicationEventPublisherAware {

    public static final String DS_DATA_SPLIT = "@";
    private static final String DS_DOMAIN_START = "//";
    private static final String DS_DOMAIN_END = "/";
    private static final String DS_PORT = ":";
    private static final String DS_POINT = ".";
    private static final String DS_END = "?";

    public static final String DS_PRIMARY = "spring.datasource.dynamic.primary";
    public static final String DS_NAME_MASTER = "master";
    public static final String DEFAULT_PORT = "5432";

    public static final String PUBLIC_KEY = "MSUN_DS_PUBLIC_KEY";

    private static final String DS_PREFIX = "msun.ds.db.";

    private static final String DS_SCHEMA = "currentSchema=";
    private static final String DS_SPLIT_OF_PARAMETER = "&";

    public static final String AUTHENTICATION = "password authentication";

    private static final String DS_SCHEMA_DIS = "msun.datasource.disable.schema.list";
    public static final String SCHEMA_ALL = "*";
    public static final String SCHEMA_SPLIT = ",";
    public static final String MSUN_DS_DRUID = "msun.ds.druid.";
    public static final char DS_CAML = '-';
    public static final String SLAVE = "slave";

    public static final String DB_BUILD_PRE = "msun.ds.key.";

    public static final String USE_ENV_KEY = "msun.ds.disable.env.key";

    private final Set<String> caches = new HashSet<>();

    @Resource
    private DataSource dataSource;

    @Resource
    private PropertiesHolder propertiesHolder;

    @Resource
    private com.baomidou.dynamic.datasource.creator.DataSourceCreator dataSourceCreator;


    /**
     * 从url的获取数据库名称
     *
     * @param url 数据库地址
     * @return 数据库名称
     */
    public static String getDb(String url) {
        int start = url.indexOf(DS_DOMAIN_START) + DS_DOMAIN_START.length();
        int s = url.indexOf(DS_DOMAIN_END, start) + 1;
        int e = url.indexOf(DS_END, s);
        if (s < 1 || e < 0) {
            return null;
        }
        return url.substring(s, e);
    }

    /**
     * 从url的获取数据库地址
     *
     * @param url 数据库地址
     * @return 数据库名称
     */
    public static String getAddr(String url) {
        int start = url.indexOf(DS_DOMAIN_START) + DS_DOMAIN_START.length();
        int end = url.indexOf(DS_DOMAIN_END, start);
        if (start < 1 || end < 0) {
            return null;
        }
        return url.substring(start, end);
    }


    /**
     * @param alias    数据源别名
     * @param fullName 数据源完整名称
     * @param domain   域名
     * @return 数据源
     */
    public synchronized DataSource createDataSource(String alias, String fullName, String domain) {
        if (!(dataSource instanceof DynamicRoutingDataSource)) {
            log.error("DataSourceCacheCreator.createDataSource.dataSource.typeError require: {} actually: {}", DynamicRoutingDataSource.class, dataSource.getClass());
            return null;
        }
        ApplicationContext context = SpringHelpers.context();
        Environment environment = context.getEnvironment();
        if (caches.contains(fullName)) {
            return ((DynamicRoutingDataSource) dataSource).getDataSource(fullName);
        }
        DataSourceProperty property = propertiesHolder.getModel(alias);

        if (buildDataSource(property, environment, domain, alias)) {
            //生产对应数据源
            property.setPoolName(fullName);
            String db = getDb(property.getUrl());
            log.info("DataSourceCacheCreator.createDataSource.dataSource.create.success,dataSource={},db={},alias={},user={}",
                    fullName, db, alias, property.getUsername());
            DataSource source = dataSourceCreator.createDataSource(property);
            ((DynamicRoutingDataSource) dataSource).addDataSource(fullName, source);
            caches.add(fullName);
            applicationEventPublisher.publishEvent(new DataSourceBuildEvent(this, source));
            return source;
        }
        log.error("DataSourceCacheCreator.createDataSource.dataSource.create.fail,dataSource={},db={},alias={},user={}",
                fullName, getDb(property.getUrl()), alias, property.getUsername());
        return null;


    }


    /**
     * 获取原有方案配置格式
     *
     * @param environment 环境
     * @param pre         前缀
     * @param alias       别名
     * @param db          数据库名称
     * @param userName    用户名称
     * @return 配置信息
     */
    private static String getData(Environment environment, String pre, String alias, String db, String userName) {
        String data = environment.getProperty(pre + db + DS_POINT + alias + DS_POINT + userName);
        if (data != null) {
            return data;
        }
        //重定向到master
        String master = environment.getProperty(DS_PRIMARY);
        if (master == null) {
            master = DS_NAME_MASTER;
        }
        data = environment.getProperty(pre + db + DS_POINT + master + DS_POINT + userName);
        if (data != null) {
            log.warn("DataSourceCacheCreator.createDataSource.config.reroute.to.master,master={},data={},dbName={},alias={}",
                    master, pre, db, alias);
            return data;
        }
        //获取失败
        return null;
    }


    /**
     * 更新数据源配置
     *
     * @param property    待更新的数据源配置
     * @param environment 环境
     * @param domain      域名
     * @param alias       别名
     * @return 是否成功
     */
    public static boolean buildDataSource(DataSourceProperty property, Environment environment, String domain, String alias) {
        final String url = property.getUrl();
        final String pre = DS_PREFIX + domain + DS_POINT;
        final String userName = property.getUsername();
        if (StringUtils.isEmpty(userName)) {
            log.error("DataSourceCacheCreator.buildDataSource.user.is.empty,property={},alias={}", pre + userName, alias);
            return false;
        }
        //读取数据库配置
        String dbMark = getAddr(url);
        //该环境的key
        String key = environment.getProperty(PUBLIC_KEY);
        DataSourceBuildProperty dataSourceBuildProperty = new DataSourceBuildProperty(domain, alias, dbMark, key);
        //新版本
        log.info("DataSourceCacheCreator.try.config.with.v2,property={}", dataSourceBuildProperty);
        try {
            if (buildWithV2(property, environment, pre, dataSourceBuildProperty)) {
                return true;
            }
        } catch (Throwable e) {
            log.error("DataSourceCacheCreator.build.with.v2.error,e=", e);
        }
        log.info("DataSourceCacheCreator.try.config.with.v1,property={}", dataSourceBuildProperty);
        //老版本
        try{
            if (buildWithV1(property, environment, pre, dataSourceBuildProperty)) {
                return true;
            }
        }catch (Throwable e){
            log.error("DataSourceCacheCreator.build.with.v1.error,e=", e);
        }
        return false;
    }

    private static boolean buildWithV2(DataSourceProperty property, Environment environment, String pre, DataSourceBuildProperty dataSourceBuildProperty) {
        String addr = environment.getProperty(pre + dataSourceBuildProperty.getDbMark());
        String pass = environment.getProperty(pre + property.getUsername());
        if (StringUtils.isEmpty(addr)) {
            log.info("DataSourceCacheCreator.buildDataSource.v2.addr.miss,property={}", dataSourceBuildProperty);
            return false;
        }
        //使用配置的医院特定密码
        String newPass = environment.getProperty(pre + property.getUsername() + DS_POINT + dataSourceBuildProperty.getDbMark());
        if (!StringUtils.isEmpty(newPass)) {
            log.info("DataSourceCacheCreator.buildDataSource.v2.try.pass,property={}", dataSourceBuildProperty);
            if (tryUpdateProperty(property, environment, dataSourceBuildProperty, addr, newPass)) {
                return true;
            }
            log.info("DataSourceCacheCreator.buildDataSource.v2.try.hospital.pass.fail,property={}", dataSourceBuildProperty);
        }
        //密码不空
        if (!StringUtils.isEmpty(pass)) {
            if (tryUpdateProperty(property, environment, dataSourceBuildProperty, addr, pass)) {
                return true;
            }
            log.info("DataSourceCacheCreator.buildDataSource.v2.try.pass.fail,property={}", dataSourceBuildProperty);
        } else {
            log.info("DataSourceCacheCreator.buildDataSource.v2.origin.pass.miss,property={}", dataSourceBuildProperty);
        }
        //尝试生成密码
        if (tryGeneratePass(property, environment, dataSourceBuildProperty, addr)) {
            return true;
        }
        log.info("DataSourceCacheCreator.buildDataSource.v2.try.generate.pass.fail,property={}", dataSourceBuildProperty);
        return false;
    }

    private static boolean buildWithV1(DataSourceProperty property, Environment environment, String pre, DataSourceBuildProperty dataSourceBuildProperty) {
        //以上条件均不满足，使用老配置
        String db = getDb(property.getUrl());
        String data = getData(environment, pre, dataSourceBuildProperty.getAlias(), db, property.getUsername());
        //配置不存在
        if (data == null) {
            log.warn("DataSourceCacheCreator.buildDataSource.v1.dataSource.not.find,property={},config={}", dataSourceBuildProperty,
                    pre + db + DS_POINT + dataSourceBuildProperty.getAlias() + DS_POINT + property.getUsername());
            return false;
        }
        int index = data.indexOf(DS_DATA_SPLIT);
        //配置格式错误
        if (index < 0) {
            log.error("DataSourceCacheCreator.buildDataSource.v1.dataSource.config.error,property={} data={}", dataSourceBuildProperty, data);
            return false;
        }
        String addr = data.substring(0, index);
        String pass = data.substring(index + 1);
        //配置完整
        if (!StringUtils.isEmpty(pass) && !StringUtils.isEmpty(addr)) {
            if (tryUpdateProperty(property, environment, dataSourceBuildProperty, addr, pass)) {
                return true;
            }
            log.info("DataSourceCacheCreator.buildDataSource.v1.try.generate.pass,property={}", dataSourceBuildProperty);
        }
        log.error("DataSourceCacheCreator.buildDataSource.v1.dataSource.config.unComplete,property={}", dataSourceBuildProperty);
        if (tryGeneratePass(property, environment, dataSourceBuildProperty, addr)) {
            return true;
        }
        log.info("DataSourceCacheCreator.buildDataSource.v1.try.generate.pass.fail,property={}", dataSourceBuildProperty);
        return false;
    }

    private static boolean buildWithOrigin(DataSourceProperty property, Environment environment, DataSourceBuildProperty dataSourceBuildProperty) {
        return tryUpdateProperty(property, environment, dataSourceBuildProperty, null, property.getPassword());
    }

    private static boolean tryGeneratePass(DataSourceProperty property, Environment environment, DataSourceBuildProperty dataSourceBuildProperty, String addr) {
        if (StringUtils.isEmpty(addr)) {
            return false;
        }
        String dbKey = environment.getProperty(DB_BUILD_PRE + dataSourceBuildProperty.getDomain());
        String useEnv = environment.getProperty(USE_ENV_KEY);
        String userName = property.getUsername();
        String key = dataSourceBuildProperty.getEnvKey();
        if (!StringUtils.isEmpty(dbKey)) {
            if (key == null) {
                key = "";
            }
            if (Boolean.TRUE.toString().equals(useEnv)) {
                key = "";
            }
            if (StringUtils.isEmpty(key)) {
                log.info("DataSourceCacheCreator.buildDataSource.try.generate.pass.with.empty.env.key!property={},user={}", dataSourceBuildProperty, userName);
            }
            String pass = buildPass(key, dbKey, userName);
            log.info("DataSourceCacheCreator.buildDataSource.try.generate.pass!dbInfo={},user={}", dataSourceBuildProperty, userName);
            return tryUpdateProperty(property, environment, dataSourceBuildProperty, addr, pass);
        } else {
            log.info("DataSourceCacheCreator.buildDataSource.does.not.try.generate.db.key.is.empty!property={},user={}", dataSourceBuildProperty, userName);
        }
        return false;
    }

    private static boolean tryUpdateProperty(DataSourceProperty property, Environment environment, DataSourceBuildProperty dataSourceBuildProperty, String addr, String pass) {
        String url = property.getUrl();
        String userName = property.getUsername();
        log.info("DataSourceCacheCreator.buildDataSource.start.update.url,property={},addr={}",
                dataSourceBuildProperty, addr);
        //更新数据库地址和密码
        String schemaList = environment.getProperty(DS_SCHEMA_DIS);
        boolean needSchema = checkSchema(userName, schemaList);
        if (!needSchema) {
            log.info("DataSourceCacheCreator.buildDataSource.not.add.schema!,user={},list={}", userName, schemaList);
        }
        url = updateUrl(url, addr);
        pass = decryptPass(dataSourceBuildProperty.getEnvKey(), pass);
        int re = checkAndUpdateDataSource(url, userName, pass, needSchema, property);
        if (re == 0) {
            updateConfig(property, environment, dataSourceBuildProperty.getDomain(), userName, dataSourceBuildProperty.getAlias());
            log.info("DataSourceCacheCreator.buildDataSource.update.url.success,property={},addr={}",
                    dataSourceBuildProperty, addr);
            return true;
        }
        if (re == 1) {
            log.error("DataSourceCacheCreator.buildDataSource.password.authentication.fail,userName={},property={}", userName, dataSourceBuildProperty);
        }
        return false;
    }

    public static void updateConfig(DataSourceProperty property, Environment environment, String domain, String userName, String alias) {
        try {
            String disableList = environment.getProperty("msun.ds.dynamic.seata.disable");
            String[] disables;
            if (!StringUtils.isEmpty(disableList)) {
                disables = disableList.split(",");
            } else {
                disables = new String[]{SLAVE};
            }
            for (String disable : disables) {
                disable = disable.trim();
                if (disable.equals(alias)) {
                    property.setSeata(false);
                }
            }
            log.info("DataSourceCacheCreator.buildDataSource.update.seata.finish,alias={},domain={},seata={}", alias, domain, property.getSeata());
            String serviceName = environment.getProperty("spring.application.name");
            //druid连接池配置注入
            Map<String, String> druidProperties = getProperties(environment, MSUN_DS_DRUID + serviceName + "." + userName + ".");
            log.info("DataSourceCacheCreator.buildDataSource.load.config,datas={}", druidProperties);
            DruidConfig config = new DruidConfig();
            DruidWallConfig wallConfig = new DruidWallConfig();
            DruidStatConfig statConfig = new DruidStatConfig();
            DruidSlf4jConfig slf4jConfig = new DruidSlf4jConfig();
            setValue(wallConfig, convertProperties(druidProperties, a -> a.startsWith("wall.")));
            setValue(statConfig, convertProperties(druidProperties, a -> a.startsWith("stat.")));
            setValue(slf4jConfig, convertProperties(druidProperties, a -> a.startsWith("slf4j.")));
            setValue(config, convertProperties(druidProperties, a -> !a.contains(".")));
            config.setWall(wallConfig);
            config.setStat(statConfig);
            config.setSlf4j(slf4jConfig);
            property.setDruid(config);
            log.info("DataSourceCacheCreator.buildDataSource.update.druid.suc,datas={}", JSON.toJSONString(config,
                    SerializerFeature.NotWriteDefaultValue));
        } catch (Throwable e) {
            log.error("DataSourceCacheCreator.buildDataSource.update.config.fail,e=", e);
        }

    }

    private static <T> void setValue(T data, Map<String, String> properties) {
        Class<?> tClass = data.getClass();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            try {
                Field field = tClass.getDeclaredField(property.getKey());
                boolean accessible = field.isAccessible();
                field.setAccessible(true);
                Class<?> filedClass = field.getType();
                field.set(data, convertValue(property.getValue(), filedClass));
                field.setAccessible(accessible);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                log.error("DataSourceCacheCreator.buildDataSource.update.config.fail,e=", e);
            }
        }
    }

    private static Object convertValue(String data, Class<?> type) {
        Object value = null;
        try {
            Method method = type.getDeclaredMethod("valueOf", String.class);
            boolean accessible = method.isAccessible();
            method.setAccessible(true);
            value = method.invoke(null, data);
            method.setAccessible(accessible);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("DataSourceCacheCreator.buildDataSource.covert.value.fail,e=", e);
        }
        return value;
    }

    private static Map<String, String> convertProperties(Map<String, String> source, Filter filter) {
        Map<String, String> res = new HashMap<>(source.size());
        for (Map.Entry<String, String> property : source.entrySet()) {
            if (filter.check(property.getKey())) {
                String key = property.getKey();
                int point = key.indexOf(DS_POINT);
                if (point > 0) {
                    key = key.substring(point + 1);
                }
                key = toCaml(key);
                res.put(key, property.getValue());
            }
        }
        return res;
    }

    private static String toCaml(String key) {
        if (StringUtils.isEmpty(key)) {
            return key;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            if (key.charAt(i) == DS_CAML) {
                if (i < key.length() - 1) {
                    i++;
                    builder.append(Character.toUpperCase(key.charAt(i)));
                }
            } else {
                builder.append(key.charAt(i));
            }
        }
        return builder.toString();
    }

    @FunctionalInterface
    private interface Filter {
        /**
         * 检查key是否符合条件
         *
         * @param key key
         * @return 是否符合
         */
        boolean check(String key);
    }


    @SuppressWarnings("unchecked")
    private static Map<String, String> getProperties(Environment environment, String prefix) {
        Map<String, String> propertyMap = new HashMap<>(16);
        if (StringUtils.isEmpty(prefix)) {
            return propertyMap;
        }
        if (environment instanceof StandardEnvironment) {
            StandardEnvironment standardEnvironment = (StandardEnvironment) environment;
            for (PropertySource<?> source : standardEnvironment.getPropertySources()) {
                Object objectSource = source.getSource();
                if (objectSource instanceof Map) {
                    Map<String, Object> properties = (Map<String, Object>) objectSource;
                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        if (entry.getKey().startsWith(prefix)) {
                            String key = entry.getKey().substring(prefix.length());
                            String data = entry.getValue().toString();
                            propertyMap.put(key, data);
                        }
                    }
                }
            }
        }
        return propertyMap;
    }

    private static String decryptPass(String key, String pass) {
        if (!StringUtils.isEmpty(key)) {
            //解密密码
            AES256TextEncryptor textEncryptor = new AES256TextEncryptor();
            textEncryptor.setPassword(key);
            try {
                pass = textEncryptor.decrypt(pass);
                log.info("DataSourceCacheCreator.buildDataSource.pass.decrypt.success!");
            } catch (Exception e) {
                log.info("DataSourceCacheCreator.buildDataSource.pass.decrypt.fail!,key={}", key);
            }
        } else {
            log.info("DataSourceCacheCreator.buildDataSource.encryptor.key.not.find!");
        }
        return pass;
    }

    private static int checkAndUpdateDataSource(String url, String userName, String pass,
                                                boolean schema, DataSourceProperty property) {
        Connection connection = null;
        int re = -1;
        try {
            connection = DriverManager.getConnection(url, userName, pass);
            if (schema) {
                url = addSchema(url, connection.getSchema());
            }
            property.setUrl(url);
            property.setPassword(pass);
            re = 0;
        } catch (SQLException e) {
            String db = getDb(url);
            log.info("DataSourceCacheCreator.checkDataSource.connect.fail,db={},user={},url={}",
                    db, userName, property.getUrl());
            if (e.getMessage() != null && e.getMessage().contains(AUTHENTICATION)) {
                re = 1;
            } else {
                log.error("DataSourceCacheCreator.checkDataSource.connect.fail,db={},user={},e={}",
                        db, userName, e);
            }

        } finally {
            JdbcUtils.close(connection);
        }
        return re;
    }

    public static boolean checkDataSource(DataSourceProperty property) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(property.getUrl(), property.getUsername(), property.getPassword());
            return true;
        } catch (SQLException e) {
            String db = getDb(property.getUrl());
            log.error("DataSourceCacheCreator.checkDataSource.connect.fail,db={},user={},e={}",
                    db, property.getUsername(), e);
            return false;
        } finally {
            JdbcUtils.close(connection);
        }
    }

    /**
     * 更新数据库连接url
     *
     * @param url  当前url
     * @param addr 目标数据库地址
     * @return 新url
     */
    private static String updateUrl(String url, String addr) {
        if (StringUtils.isEmpty(addr)) {
            return url;
        }
        int start = url.indexOf(DS_DOMAIN_START) + DS_DOMAIN_START.length();
        int end = url.indexOf(DS_DOMAIN_END, start);
        if (!addr.contains(DS_PORT)) {
            addr += DS_PORT + DEFAULT_PORT;
            log.warn("DataSourceCacheCreator.updateUrl.addr.unComplete.miss.port,addr={},backPort={}", addr, DEFAULT_PORT);
        }
        return url.substring(0, start) + addr + url.substring(end);
    }


    /**
     * 添加currentSchema到url中
     *
     * @param url    连接url
     * @param schema schema
     * @return 更新后的url
     */
    private static String addSchema(String url, String schema) {

        if (StringUtils.isEmpty(schema)) {
            log.error("DataSourceCacheCreator.dataSource.schema.not.exists,url={}", url);
            return url;
        }
        //包含schema，无需添加
        if (url.contains(DS_SCHEMA)) {
            log.info("DataSourceCacheCreator.dataSource.schema.exists.in.url,schema={},url={}", schema, url);
            return url;
        }
        //添加schema信息
        int point = url.indexOf(DS_END);
        if (point > 0) {
            url = url.substring(0, point + 1) + DS_SCHEMA + schema + DS_SPLIT_OF_PARAMETER + url.substring(point + 1);
            log.info("DataSourceCacheCreator.dataSource.add.schema.to.url,schema={},url={}", schema, url);
            return url;
        }
        return url;
    }

    private static boolean checkSchema(String user, String list) {
        if (StringUtils.isEmpty(list)) {
            return true;
        }
        if (list.contains(SCHEMA_ALL)) {
            return false;
        }
        String[] datas = list.split(SCHEMA_SPLIT);
        for (String data : datas) {
            if (user.equals(data)) {
                return false;
            }
        }
        return true;
    }

    public static final String BASE = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_+";
    public static final int LENGTH = 24;
    public static final String SALT_A = "msun-pass";
    public static final String SALT_B = "second-salt";

    public static String buildPass(String key, String dbKey, String userName) {
        log.info("DataSourceCacheCreator.buildPass.build.with.user={}", userName);
        MD5 md5 = MD5.create();
        String info = md5.digestHex(SALT_A + dbKey + userName);
        info = md5.digestHex(SALT_B + info + key + dbKey);
        StringBuilder res = new StringBuilder();
        long seedA;
        long seedB;
        seedA = Long.parseLong(info.substring(0, 15), 16);
        seedB = Long.parseLong(info.substring(15, 30), 16);
        RandomBuild a = new RandomBuild(seedA);
        RandomBuild b = new RandomBuild(seedB);
        for (int i = 0; i < LENGTH; i++) {
            RandomBuild random;
            if (i % 2 == 0) {
                random = a;
            } else {
                random = b;
            }
            int range = BASE.length();
            int offset = random.nextInt(range);
            char data = BASE.charAt(offset);
            res.append(data);
        }
        return res.toString();
    }

    private static class RandomBuild {
        public static final int INT_BITS = 31;
        private long seed;

        private static final long MULTIPLIER = 0x5DEECE66DL;
        private static final long ADDEND = 0xBL;
        public static final int COUNT = 48;
        private static final long MASK = (1L << COUNT) - 1;

        public RandomBuild(long seed) {
            this.seed = seed;
        }

        private int next() {
            seed = (seed * MULTIPLIER + ADDEND) & MASK;
            return (int) (seed >>> (COUNT - INT_BITS));
        }

        public int nextInt(int bound) {
            int r = next();
            int m = bound - 1;
            if ((bound & m) == 0)  // i.e., bound is a power of 2
            {
                r = (int) ((bound * (long) r) >> INT_BITS);
            } else {
                int u = r;
                while (u - (r = u % bound) + m < 0) {
                    u = next();
                }
            }
            return r;
        }
    }


    ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
