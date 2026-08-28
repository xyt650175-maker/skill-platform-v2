package com.company.llmaif.skills.service.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 创建智能体版本 DTO
 */
@Data
public class CreateAgentVersionDTO {

    @NotBlank(message = "版本号不能为空")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "版本号需使用 x.y.z 格式")
    private String version;

    private String changeSummary;
}
