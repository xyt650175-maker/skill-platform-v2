package com.company.llmaif.skills.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/** 审批记录与 Skill 定版动作分离，避免提交评审时提前发布。 */
@Data
@TableName("skill_review")
public class SkillReviewEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long skillId;
    private Long versionId;
    private String status;
    private String comment;
    private Long applicantId;
    private Long reviewerId;
    // 调试结果
    private String debugResult;
    private Boolean debugPassed;
    private String debugInput;
    private Integer totalTokens;
    private Integer testCaseCount;
    private Integer testPassCount;
    private String debugSummary;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
