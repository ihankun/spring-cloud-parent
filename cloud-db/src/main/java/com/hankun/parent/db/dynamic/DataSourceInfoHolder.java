package com.hankun.parent.db.dynamic;

import com.hankun.parent.db.dynamic.bean.KunDataSourceInfo;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;

/**
 * @author hankun
 */
@Slf4j
@Component
public class DataSourceInfoHolder {

    public static final String PUBLIC_KEY = "MSUN_DS_PUBLIC_KEY";
    private static final String DS_PREFIX = "msun.ds.db.";
    private static final String DS_POINT = ".";

    @Resource
    Environment environment;

    /**
     * 根据域名、数据库名称(HIS_MASTER等)、用户名，获取匹配的数据库ip端口密码信息
     * @param domain    域名
     * @param db        数据库名称(HIS_MASTER等)
     * @param userName  用户名
     * @return com.msun.core.db.dynamic.bean.MsunDataSourceInfo
     */
    public KunDataSourceInfo getDataSourceInfo(@NotEmpty String domain, @NotEmpty String db, @NotEmpty String userName) {
        final String pre = DS_PREFIX + domain + DS_POINT;

        String ipPort = environment.getProperty(pre + db);
        if (StringUtils.isBlank(ipPort)) {
            log.error("DataSourceInfoHolder.getDataSourceInfo.ipPort.get.fail,domain={},db={},user={}", domain, db, userName);
            return null;
        }

        String password = environment.getProperty(pre + userName + DS_POINT + db);
        if (StringUtils.isBlank(password)) {
            log.warn("DataSourceInfoHolder.getDataSourceInfo.get.password.from.database.null,domain={},db={},user={}", domain, db, userName);

            password = environment.getProperty(pre + userName);
            if (StringUtils.isBlank(password)) {
                log.error("DataSourceInfoHolder.getDataSourceInfo.get.password.null,domain={},db={},user={}", domain, db, userName);
                return null;
            }
        }

        String key = environment.getProperty(PUBLIC_KEY);
        password = decryptPass(key, password);
        log.info("DataSourceInfoHolder.getDataSourceInfo.get.password.success,domain={},db={},user={},pass={}", domain, db, userName, password);

        String[] ipPortSplit = ipPort.split(":");
        return KunDataSourceInfo.builder().
                ip(ipPortSplit[0]).
                port(ipPortSplit[1]).
                password(password).
                build();
    }

    private static String decryptPass(String key, String pass) {
        if (StringUtils.isNotBlank(key)) {
            // 解密密码
            AES256TextEncryptor textEncryptor = new AES256TextEncryptor();
            textEncryptor.setPassword(key);
            try {
                pass = textEncryptor.decrypt(pass);
                log.info("DataSourceInfoHolder.getDataSourceInfo.pass.decrypt.success!");
            } catch (Exception e) {
                log.info("DataSourceInfoHolder.getDataSourceInfo.pass.decrypt.fail!");
            }
        } else {
            log.info("DataSourceInfoHolder.getDataSourceInfo.encryptor.key.not.find!");
        }
        return pass;
    }
}
