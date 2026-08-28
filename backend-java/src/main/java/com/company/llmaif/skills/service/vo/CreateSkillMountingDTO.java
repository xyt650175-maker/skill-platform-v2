package com.company.llmaif.skills.service.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CreateSkillMountingDTO {
    @NotNull(message = "Skill ID 不能为空")
    private Long skillId;

    private Long subAgentId;

    private String skillAlias;

    @NotNull(message = "智能体版本不能为空")
    private String agentVersion;

    private Boolean enabled = true;
}
