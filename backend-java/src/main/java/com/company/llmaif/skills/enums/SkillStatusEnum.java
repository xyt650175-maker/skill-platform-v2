package com.company.llmaif.skills.enums;

import lombok.Getter;

/**
 * Skill 状态枚举
 */
@Getter
public enum SkillStatusEnum {

    DRAFT("draft", "草稿"),
    REVIEWING("reviewing", "评审中"),
    TESTING("testing", "测试中"),
    RELEASED("released", "已发布");

    private final String code;
    private final String desc;

    SkillStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SkillStatusEnum fromCode(String code) {
        for (SkillStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
