package com.company.llmaif.skills.service.vo;

import lombok.Data;

import java.util.Date;

@Data
public class SubAgentVO {
    private Long id;
    private Long agentId;
    private String name;
    private String description;
    private String systemPrompt;
    private String modelName;
    private Date createTime;
    private Date updateTime;
}
