package com.company.llmaif.git.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 企业内网 Git 仓库连接配置。令牌仅保存于后端，不会返回浏览器。 */
@Data
@TableName("git_credential")
public class GitCredentialEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private String name;
    private String repoUrl;
    private String authType;
    private String username;
    private String secretCiphertext;
    private Long creatorId;
    @TableLogic
    private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
