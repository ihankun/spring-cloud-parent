package com.hankun.parent.springcloud.server.convert;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;

import java.util.List;

/**
 * 功能描述: DTO-VO通用转换类
 * @author hankun
 */
public interface Dto2VoBaseConvert<Dto, Vo> {

    /**
     * 将单个DTO转为VO
     *
     * @param dto
     * @return VO
     */
    @InheritConfiguration
    Vo dto2Vo(Dto dto);

    /**
     * 将DTO集合转为VO集合
     *
     * @param sourceList
     * @return VOList
     */
    @InheritConfiguration
    List<Vo> dto2Vo(List<Dto> sourceList);

    /**
     * 将单个VO转为DTO
     *
     * @param vo
     * @return DTO
     */
    @InheritInverseConfiguration
    Dto vo2Dto(Vo vo);

    /**
     * 将target集合转为集合source集合
     *
     * @param voList
     * @return dtoList集合
     */
    @InheritInverseConfiguration
    List<Dto> vo2Dto(List<Vo> voList);
}
