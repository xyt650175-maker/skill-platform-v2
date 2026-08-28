package com.company.llmaif.skills.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Skill 操作日志实体类
 */
@Data
@TableName("skill_operation_log")
public class SkillOperationLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long skillId;

    private Long operatorId;

    private String operatorName;

    private String action;

    private String description;

    private String changeSummary;

    private String status;

    private String version;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
