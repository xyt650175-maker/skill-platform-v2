package com.company.llmaif.skills.service.vo;

import lombok.Data;

import java.util.Date;

/**
 * Skill 操作日志 VO
 */
@Data
public class SkillOperationLogVO {
    private Long id;
    private Long skillId;
    private Long operatorId;
    private String operatorName;
    private String action;
    private String actionLabel;
    private String description;
    private String changeSummary;
    private String status;
    private String version;
    private Date createTime;
}
