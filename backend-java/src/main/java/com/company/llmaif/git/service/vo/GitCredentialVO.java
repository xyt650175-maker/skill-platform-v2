package com.company.llmaif.git.service.vo;

import lombok.Data;

@Data
public class GitCredentialVO {
    private Long id;
    private String name;
    private String repoUrl;
    private String authType;
    private String username;
    private String tokenMasked;
    private String connectionStatus;
}
