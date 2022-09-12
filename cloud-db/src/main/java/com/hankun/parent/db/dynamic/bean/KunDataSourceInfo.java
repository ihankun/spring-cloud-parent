package com.hankun.parent.db.dynamic.bean;

import lombok.Builder;
import lombok.Data;

/**
 * @author hankun
 */
@Data
@Builder
public class KunDataSourceInfo {

    private String ip;

    private String port;

    private String password;
}
