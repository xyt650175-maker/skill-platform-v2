package com.company.llmaif.skills.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.llmaif.skills.dao.entity.SkillOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Skill 操作日志 DAO
 */
@Mapper
public interface SkillOperationLogDAO extends BaseMapper<SkillOperationLogEntity> {

    IPage<SkillOperationLogEntity> selectPageBySkillId(Page<SkillOperationLogEntity> page, @Param("skillId") Long skillId);
}
