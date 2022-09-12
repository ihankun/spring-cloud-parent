package com.hankun.parent.springcloud.server.global;

import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;
import com.hankun.parent.commons.error.CommonErrorCode;
import com.hankun.parent.commons.exception.BusinessException;
import io.seata.common.util.CollectionUtils;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(value = "msun.domain")
@RefreshScope
public class DomainHolder {

    @Setter
    private Map<String, DomainItem> config;


    public List<DomainItem> getAllHospital(boolean supportJob) {
        if (config == null) {
            throw BusinessException.build(CommonErrorCode.DOMAIN_NOT_CONFIG);
        }

        List<DomainItem> list = config.values().stream().collect(Collectors.toList());

        if (!supportJob) {
            List<DomainItem> collect = list.stream().filter(item -> item.getIsSupportJob() == null || item.getIsSupportJob().booleanValue() == true).collect(Collectors.toList());
            return collect;
        }

        return list;
    }

    /**
     * 获取所有医院
     *
     * @return
     */
    public List<DomainItem> getAllHospital() {
        if (config == null) {
            throw BusinessException.build(CommonErrorCode.DOMAIN_NOT_CONFIG);
        }

        List<DomainItem> list = config.values().stream().collect(Collectors.toList());
        return list;
    }


    /**
     * 根据Host获取配置列表
     * host跟医院ID存在一对多的情况
     *
     * @param host 域名
     * @return
     */
    public List<DomainItem> getByHost(String host) {
        if (config == null) {
            throw BusinessException.build(CommonErrorCode.DOMAIN_NOT_CONFIG);
        }

        List<DomainItem> domainItemList = config.values().stream().filter(
                item -> item.getHost().equals(host)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(domainItemList)) {
            throw BusinessException.build(CommonErrorCode.DOMAIN_NOT_FOUND);
        }
        return domainItemList;
    }


    /**
     * 根据当前登录用户信息获取域名配置信息
     *
     * @return
     */
    public DomainItem getByLoginUserInfo() {

        LoginUserInfo userInfo = LoginUserContext.getLoginUserInfo();
        if (userInfo == null) {
            throw BusinessException.build(CommonErrorCode.USER_NOT_LOGIN);
        }

        Long orgId = userInfo.getOrgId();
        return getById(orgId);
    }

    /**
     * 根据医院ID获取配置
     * 医院ID和host是一对一
     *
     * @param orgId
     * @return
     */
    public DomainItem getById(Long orgId) {
        if (config == null) {
            throw BusinessException.build(CommonErrorCode.DOMAIN_NOT_CONFIG);
        }

        for (DomainItem item : config.values()) {
            if (item.getOrgId().equals(orgId)) {
                return item;
            }
        }
        return new DomainItem();
    }


    /**
     * 根据顶层机构ID查询对应的医院信息配置
     *
     * @param msunOrgId
     * @return
     */
    public DomainItem getByOrgId(String msunOrgId) {
        if (config == null) {
            throw BusinessException.build(CommonErrorCode.DOMAIN_NOT_CONFIG);
        }
        if (!StringUtils.isEmpty(msunOrgId)) {
            for (DomainItem item : config.values()) {
                if (msunOrgId.equals(item.getMsunOrgId())) {
                    return item;
                }
            }
        }
        return null;
    }
}
