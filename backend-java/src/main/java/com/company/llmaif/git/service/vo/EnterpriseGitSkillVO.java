package com.company.llmaif.git.service.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** 企业内网 Git 中可导入的标准 Skill 包。 */
@Data
public class EnterpriseGitSkillVO {
    private String path;
    private String name;
    private String version;
    private String description;
    /** 仅包含 SKILL.md、requirements.txt、scripts/、references/ 与 assets/ 下的文本文件。 */
    private Map<String, String> files = new LinkedHashMap<>();
}
