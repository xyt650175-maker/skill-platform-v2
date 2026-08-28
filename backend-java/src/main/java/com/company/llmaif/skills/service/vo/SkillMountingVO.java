package com.company.llmaif.skills.service.vo;

import lombok.Data;

import java.util.Date;

@Data
public class SkillMountingVO {
    private Long id;
    private Long agentId;
    private Long subAgentId;
    private Long skillId;
    private String skillName;
    private String skillAlias;
    private String agentVersion;
    private Boolean enabled;
    private Date createTime;
    private Date updateTime;
}
