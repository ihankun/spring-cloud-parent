package com.hankun.parent.commons.api;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author hankun
 */
@Data
@AllArgsConstructor
public class BaseMiddleDTO implements Serializable {

    @ApiModelProperty("机构ID，必填")
    @NotNull
    private Long orgId;

    public BaseMiddleDTO() {
    }

}
