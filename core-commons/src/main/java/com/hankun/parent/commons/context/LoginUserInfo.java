package com.hankun.parent.commons.context;

import com.hankun.parent.commons.api.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class LoginUserInfo extends BaseEntity {

    @ApiModelProperty("机构ID")
    private Long orgId;

    @ApiModelProperty("机构名称")
    private String orgName;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户名称")
    private String userName;

    @ApiModelProperty("账号ID")
    private Long accountId;

    @ApiModelProperty("用户名称")
    private String accountName;

    @ApiModelProperty("用户身份ID")
    private Long userSysId;

    @ApiModelProperty("用户身份名称")
    private String userSysName;

    @ApiModelProperty("用户选择的系统ID")
    private String systemId;

    @ApiModelProperty("是否是超级管理员")
    private boolean superUser;

    @ApiModelProperty("登录后Token")
    private String token;

    @ApiModelProperty("登录后秘钥")
    private String secret;

    @ApiModelProperty("用户允许访问的域名列表，英文逗号分隔")
    private String allowDomains;

    @ApiModelProperty("设备地址")
    private String deviceMac;

    @ApiModelProperty("设备IP")
    private String deviceIp;
}
