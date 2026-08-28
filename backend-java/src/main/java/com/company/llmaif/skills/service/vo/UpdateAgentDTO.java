package com.company.llmaif.skills.service.vo;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateAgentDTO {
    private Long id;

    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

    private String status;
    private String modelName;
    private String systemPrompt;
    private String canvasConfig;
}
