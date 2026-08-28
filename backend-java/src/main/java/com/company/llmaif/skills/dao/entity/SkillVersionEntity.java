package com.company.llmaif.skills.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Skill 版本实体类
 */
@Data
@TableName("skill_version")
public class SkillVersionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long skillId;

    private String version;

    private String sourceType;

    private String sourceRef;

    private String changeSummary;

    private String snapshotPath;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
