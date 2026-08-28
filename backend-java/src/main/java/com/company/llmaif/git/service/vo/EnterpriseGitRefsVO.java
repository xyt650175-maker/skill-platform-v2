package com.company.llmaif.git.service.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** 企业 Git 远程仓库的可选分支与 Tag。 */
@Data
public class EnterpriseGitRefsVO {
    private List<String> branches = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
}
