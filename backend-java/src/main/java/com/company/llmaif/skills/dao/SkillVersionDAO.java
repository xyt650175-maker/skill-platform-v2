package com.company.llmaif.skills.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.skills.dao.entity.SkillVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Skill 版本 DAO
 */
@Mapper
public interface SkillVersionDAO extends BaseMapper<SkillVersionEntity> {

    @Select("SELECT * FROM skill_version WHERE skill_id = #{skillId} ORDER BY create_time DESC")
    List<SkillVersionEntity> selectBySkillId(@Param("skillId") Long skillId);

    @Select("SELECT * FROM skill_version WHERE skill_id = #{skillId} AND version = #{version}")
    SkillVersionEntity selectBySkillIdAndVersion(@Param("skillId") Long skillId, @Param("version") String version);
}
