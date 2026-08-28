package com.company.llmaif.skills.service.vo;

import lombok.Data;

import java.util.Date;

/**
 * 智能体版本 VO
 */
@Data
public class AgentVersionVO {
    private Long id;
    private Long agentId;
    private String version;
    private String changeSummary;
    private Long creatorId;
    private String creatorName;
    private Date createTime;
}
