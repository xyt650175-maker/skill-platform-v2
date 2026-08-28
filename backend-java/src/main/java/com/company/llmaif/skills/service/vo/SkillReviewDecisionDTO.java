package com.company.llmaif.skills.service.vo;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class SkillReviewDecisionDTO {
    @NotBlank(message = "评审结论不能为空")
    private String decision;
    private String comment;
}
