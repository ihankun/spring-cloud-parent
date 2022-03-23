package com.hankun.parent.commons.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtTokenUtil {

    /**
     * token分隔符
     */
    private static final String SPLIT = "@@@";

    /**
     * token分隔后长度
     */
    private static final int SPLIT_LENGTH = 2;

    private static final String SECRET = "jwt-key";
}
