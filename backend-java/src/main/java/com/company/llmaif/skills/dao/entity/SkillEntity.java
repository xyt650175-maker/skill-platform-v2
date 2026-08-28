package com.company.llmaif.skills.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Skill 实体类
 */
@Data
@TableName("skill")
public class SkillEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Skill 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 运行时：python/java/node
     */
    private String runtime;

    /**
     * Git 仓库路径
     */
    private String gitRepoPath;

    /**
     * 当前版本
     */
    private String currentVersion;

    /** 草稿修订号。客户端保存时携带该值，可检测多人覆盖。 */
    private Long draftRevision;

    /**
     * 状态：draft/testing/released
     */
    private String status;

    /**
     * 可见范围：public/private/team
     */
    private String visibility;

    /**
     * 入口文件
     */
    private String entryFile;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
