package com.company.llmaif.skills.service.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateSubAgentDTO {
    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称长度不能超过128个字符")
    private String name;

    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

    @NotBlank(message = "系统提示词不能为空")
    private String systemPrompt;

    private String modelName;
}
