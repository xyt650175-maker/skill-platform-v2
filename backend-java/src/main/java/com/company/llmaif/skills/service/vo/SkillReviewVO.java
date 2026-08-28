package com.company.llmaif.skills.service.vo;

import lombok.Data;
import java.util.Date;

@Data
public class SkillReviewVO {
    private Long id;
    private Long skillId;
    private String skillName;
    private String version;
    private String status;
    private String comment;
    private Long applicantId;
    private String applicantName;
    private Long reviewerId;
    private String reviewerName;
    private Date createTime;
    private Date updateTime;
    // 调试结果
    private String debugResult;
    private Boolean debugPassed;
    private String debugInput;
    private Integer totalTokens;
    private Integer testCaseCount;
    private Integer testPassCount;
    private String debugSummary;
}
