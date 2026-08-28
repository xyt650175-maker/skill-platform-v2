package com.company.llmaif.skills.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.skills.dao.entity.PipelineEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PipelineDAO extends BaseMapper<PipelineEntity> {
    PipelineEntity selectByName(@Param("name") String name);
    List<PipelineEntity> selectByType(@Param("type") String type);
}
