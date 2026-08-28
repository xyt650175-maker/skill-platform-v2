package com.company.llmaif.skills.service.vo;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CreateEvalTaskDTO {
    @NotBlank(message = "任务名称不能为空")
    private String name;

    @NotNull(message = "Agent ID 不能为空")
    private Long agentId;

    private String agentVersion;

    @NotBlank(message = "测评集 key 不能为空")
    private String datasetKey;

    private String datasetVersion;

    private String scope = "all";
}
