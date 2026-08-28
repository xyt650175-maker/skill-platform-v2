package com.company.llmaif.skills.service.vo;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class CreatePipelineDTO {
    @NotBlank(message = "流水线名称不能为空")
    private String name;

    private String description;

    private String type = "factory";

    /** 更新时可传；创建时由后端默认 draft。 */
    private String status;

    @NotBlank(message = "阶段配置不能为空")
    private String stages;
}
