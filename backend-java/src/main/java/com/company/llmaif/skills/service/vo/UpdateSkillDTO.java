package com.company.llmaif.skills.service.vo;

import lombok.Data;

import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 更新 Skill 请求参数
 */
@Data
public class UpdateSkillDTO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 描述
     */
    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

    /**
     * 运行时
     */
    private String runtime;

    /**
     * Git 仓库路径
     */
    private String gitRepoPath;

    /**
     * 状态
     */
    private String status;

    /**
     * 入口文件
     */
    private String entryFile;
}
