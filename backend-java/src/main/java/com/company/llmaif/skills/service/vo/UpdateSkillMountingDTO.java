package com.company.llmaif.skills.service.vo;

import lombok.Data;

/**
 * 更新 Skill 挂载 DTO
 */
@Data
public class UpdateSkillMountingDTO {
    private String agentVersion;
    private String skillAlias;
    private Boolean enabled;
}
