package com.hankun.parent.commons.api;

import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class BasePageVO<T> extends BaseEntity {

    @ApiModelProperty("总条数")
    private Long total;

    @ApiModelProperty("当前页数")
    private Integer pageNum;

    @ApiModelProperty("每页数量")
    private Integer pageSize;

    @ApiModelProperty("分页数据")
    private List<T> list;

    public BasePageVO() {
    }
}
