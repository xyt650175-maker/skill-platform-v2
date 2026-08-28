package com.company.llmaif.skills.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.skills.dao.entity.EvalTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface EvalTaskDAO extends BaseMapper<EvalTaskEntity> {
    List<EvalTaskEntity> selectByAgentId(@Param("agentId") Long agentId);
    List<EvalTaskEntity> selectByStatus(@Param("status") String status);
}
