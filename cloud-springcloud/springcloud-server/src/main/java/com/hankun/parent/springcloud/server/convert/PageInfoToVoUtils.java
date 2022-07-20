package com.hankun.parent.springcloud.server.convert;

import com.github.pagehelper.PageInfo;
import com.hankun.parent.commons.api.BasePageVO;

public class PageInfoToVoUtils {

    /**
     * 转换
     */
    public static <T> BasePageVO<T> convert(PageInfo<T> pageInfo) {

        BasePageVO vo = new BasePageVO<>();

        vo.setTotal(pageInfo.getTotal());
        vo.setPageSize(pageInfo.getPageSize());
        vo.setPageNum(pageInfo.getPageNum());
        vo.setList(pageInfo.getList());

        return vo;
    }
}
