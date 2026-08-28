package com.company.llmaif.skills.service.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** Skill 草稿文件内容。 */
@Data
public class SkillCodeVO {

    private String code;
    private String skillsMd;
    private Long draftRevision;
    private Map<String, String> files = new LinkedHashMap<>();
}
