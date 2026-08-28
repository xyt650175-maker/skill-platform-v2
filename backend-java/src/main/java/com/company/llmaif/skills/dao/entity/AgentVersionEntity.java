package com.company.llmaif.skills.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 智能体版本实体类
 */
@Data
@TableName("agent_version")
public class AgentVersionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private String version;

    private String changeSummary;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
