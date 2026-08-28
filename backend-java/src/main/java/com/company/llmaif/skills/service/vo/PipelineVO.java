package com.company.llmaif.skills.service.vo;

import lombok.Data;
import java.util.Date;

@Data
public class PipelineVO {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String stages;
    private String currentVersion;
    private String status;
    private String statusDesc;
    private Long creatorId;
    private Date createTime;
    private Date updateTime;
}
