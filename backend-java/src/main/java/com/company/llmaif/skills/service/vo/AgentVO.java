package com.company.llmaif.skills.service.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AgentVO {
    private Long id;
    private String name;
    private String description;
    private String currentVersion;
    private String status;
    private String statusDesc;
    private String modelName;
    private String systemPrompt;
    private String canvasConfig;
    private Long creatorId;
    private String creatorName;
    private Date createTime;
    private Date updateTime;
    private List<AgentVersionVO> versions;
}
