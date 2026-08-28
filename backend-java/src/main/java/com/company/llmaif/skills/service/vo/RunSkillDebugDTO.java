package com.company.llmaif.skills.service.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Skill 调试请求。inputJson 必须是一个 JSON 对象。 */
@Data
public class RunSkillDebugDTO {

    @NotBlank(message = "调试输入不能为空")
    @Size(max = 64 * 1024, message = "调试输入不能超过 64KB")
    private String inputJson;
}
