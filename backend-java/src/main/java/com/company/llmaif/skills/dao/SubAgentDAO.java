package com.company.llmaif.skills.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.skills.dao.entity.SubAgentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubAgentDAO extends BaseMapper<SubAgentEntity> {

    List<SubAgentEntity> selectByAgentId(@Param("agentId") Long agentId);
}
