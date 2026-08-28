package com.company.llmaif.skills.service.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 保存 Skill 草稿代码的请求参数。 */
@Data
public class SaveSkillCodeDTO {

    @NotNull(message = "代码内容不能为空")
    @Size(max = 2 * 1024 * 1024, message = "单个文件不能超过 2MB")
    private String code;

    @NotBlank(message = "入口文件不能为空")
    @Size(max = 256, message = "入口文件路径不能超过 256 个字符")
    private String entryFile = "scripts/main.py";

    /** 可选的客户端草稿修订号；不一致时拒绝覆盖并提示刷新。 */
    private Long expectedRevision;
}
