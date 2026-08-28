package com.company.llmaif.skills.service.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;

/**
 * 创建 Skill 请求参数
 */
@Data
public class CreateSkillDTO {

    /**
     * Skill 名称
     */
    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称长度不能超过128个字符")
    private String name;

    /**
     * 描述
     */
    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

    /**
     * 运行时
     */
    private String runtime = "python";

    /**
     * Git 仓库路径
     */
    private String gitRepoPath;

    /**
     * 入口文件
     */
    private String entryFile = "scripts/main.py";

    /** 初始版本，发布时会写入 SKILL.md 并创建同名 Git Tag。 */
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "版本必须符合 x.y.z 格式")
    private String version = "0.0.0";

    /**
     * 可见范围：public/private/team
     */
    private String visibility = "private";
}
