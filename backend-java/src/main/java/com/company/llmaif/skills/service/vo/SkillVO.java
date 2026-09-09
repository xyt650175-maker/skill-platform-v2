package com.company.llmaif.skills.service.vo;

import lombok.Data;

import java.util.Date;

/**
 * Skill 视图对象
 */
@Data
public class SkillVO {

    /**
     * 主键
     */
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
     * 运行时
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

    /**
     * 状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 可见范围：public/private/team
     */
    private String visibility;

    /**
     * 入口文件
     */
    private String entryFile;

    /**
     * 运行目标平台，逗号分隔
     */
    private String targetPlatforms;

    /**
     * 依赖摘要 JSON
     */
    private String dependencySummary;

    /**
     * 关联智能体 ID（从 skill_mounting 反查，可空）
     */
    private Long agentId;

    /**
     * 关联智能体名称
     */
    private String agentName;

    /**
     * 关联智能体版本
     */
    private String agentVersion;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建人名称
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
