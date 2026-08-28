package com.company.llmaif.git.service.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GitCredentialDTO {
    private Long id;
    @NotBlank(message = "凭证名称不能为空")
    private String name;
    @NotBlank(message = "仓库地址不能为空")
    private String repoUrl;
    private String username;
    /** 仅写入，不会从任何查询接口返回。更新已有配置时可留空以保留原令牌。 */
    private String token;
}
