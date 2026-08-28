package com.company.llmaif.skills.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.skills.dao.entity.AgentVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 智能体版本 DAO
 */
@Mapper
public interface AgentVersionDAO extends BaseMapper<AgentVersionEntity> {

    @Select("SELECT * FROM agent_version WHERE agent_id = #{agentId} ORDER BY create_time DESC")
    List<AgentVersionEntity> selectByAgentId(@Param("agentId") Long agentId);

    @Select("SELECT * FROM agent_version WHERE agent_id = #{agentId} AND version = #{version}")
    AgentVersionEntity selectByAgentIdAndVersion(@Param("agentId") Long agentId, @Param("version") String version);
}
