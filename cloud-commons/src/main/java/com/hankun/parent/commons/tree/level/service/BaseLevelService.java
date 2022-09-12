package com.hankun.parent.commons.tree.level.service;

import com.hankun.parent.commons.api.BaseService;
import com.hankun.parent.commons.error.CommonErrorCode;
import com.hankun.parent.commons.exception.BusinessException;
import com.hankun.parent.commons.tree.level.IBaseLevelDTO;
import com.hankun.parent.commons.tree.level.IBaseLevelVO;
import com.hankun.parent.commons.tree.level.enums.LevelEnum;

public interface BaseLevelService extends BaseService {

    /**
     * 设置级别
     *
     * @param levelDTO
     */
    default void setLevel(IBaseLevelDTO levelDTO) {
        if (levelDTO == null) {
            return;
        }

        //如果是保存的话
        if (levelDTO.getId() == null) {

            //有上级信息的话
            if (levelDTO.getParentId() != null) {

                //查询上级信息
                IBaseLevelVO parent = getBaseLevelVoById(levelDTO.getParentId());
                levelDTO.setLevel(parent.getLevel() + 1);
                return;
            }

            //没有上级的话设置为一级
            levelDTO.setLevel(LevelEnum.FIRST.getLevel());
            return;
        }

        //更新操作 且 没有上级信息的话
        if (levelDTO.getParentId() == null) {

            //查询数据库中的权限信息
            IBaseLevelVO oldRole = getBaseLevelVoById(levelDTO.getId());

            //当前角色如果之前有上级，后续将上级删掉的话，级别设置为1
            if (oldRole.getParentId() != null) {
                levelDTO.setLevel(LevelEnum.FIRST.getLevel());
            }
            return;
        }

        //更新操作 且 有上级的话
        IBaseLevelVO parent = getBaseLevelVoById(levelDTO.getParentId());
        if (parent == null) {
            throw BusinessException.build(CommonErrorCode.UP_LEVEL_NOT_FOUND);
        }
        levelDTO.setLevel(parent.getLevel() + 1);
    }

    /**
     * 根据id获取BaseLevelVo
     *
     * @param id 主键
     * @return
     */
    IBaseLevelVO getBaseLevelVoById(Long id);
}
