package com.company.llmaif.skills.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.skills.dao.entity.SkillMountingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkillMountingDAO extends BaseMapper<SkillMountingEntity> {

    List<SkillMountingEntity> selectByAgentId(@Param("agentId") Long agentId);

    List<SkillMountingEntity> selectByAgentIdAndVersion(@Param("agentId") Long agentId, @Param("agentVersion") String agentVersion);

    List<SkillMountingEntity> selectBySkillId(@Param("skillId") Long skillId);
}
