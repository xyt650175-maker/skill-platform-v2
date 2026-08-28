package com.company.llmaif.skills.service.vo;

import lombok.Data;

import java.util.Date;

/**
 * Skill 版本 VO
 */
@Data
public class SkillVersionVO {
    private Long id;
    private Long skillId;
    private String version;
    private String sourceType;
    private String sourceRef;
    private String changeSummary;
    private String snapshotPath;
    private Long creatorId;
    private String creatorName;
    private Date createTime;
}
