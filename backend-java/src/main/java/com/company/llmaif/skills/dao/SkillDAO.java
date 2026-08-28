package com.company.llmaif.skills.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.skills.dao.entity.SkillEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Skill DAO 接口
 */
@Mapper
public interface SkillDAO extends BaseMapper<SkillEntity> {

    /**
     * 根据名称查询 Skill
     */
    SkillEntity selectByName(@Param("name") String name);

    /**
     * 查询用户的 Skill 列表
     */
    List<SkillEntity> selectByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 根据状态查询 Skill 列表
     */
    List<SkillEntity> selectByStatus(@Param("status") String status);
}
