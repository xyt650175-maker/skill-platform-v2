package com.company.llmaif.skills.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.skills.dao.entity.AgentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentDAO extends BaseMapper<AgentEntity> {

    AgentEntity selectByName(@Param("name") String name);

    List<AgentEntity> selectByCreatorId(@Param("creatorId") Long creatorId);

    List<AgentEntity> selectByStatus(@Param("status") String status);
}
