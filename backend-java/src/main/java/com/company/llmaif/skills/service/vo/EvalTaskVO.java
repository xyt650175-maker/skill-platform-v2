package com.company.llmaif.skills.service.vo;

import lombok.Data;
import java.util.Date;

@Data
public class EvalTaskVO {
    private Long id;
    private String name;
    private Long agentId;
    private String agentName;
    private String agentVersion;
    private String datasetKey;
    private String datasetVersion;
    private String scope;
    private String status;
    private String statusDesc;
    private String resultSummary;
    private Long creatorId;
    private Date createTime;
    private Date updateTime;
}
