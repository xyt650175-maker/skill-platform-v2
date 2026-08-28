package com.company.llmaif.skills.service.vo;

import lombok.Data;

/** 一次 Skill 调试的结果。 */
@Data
public class SkillDebugVO {

    /** PASS / FAILED */
    private String status;
    /** 运行方式。当前为受限本地运行，不等同于容器级安全沙箱。 */
    private String executionMode;
    private long durationMs;
    /** 进程退出码；启动前校验失败或超时时为空。 */
    private Integer exitCode;
    private int dependencyCalls;
    private String output;
    /** 面向界面的失败摘要。 */
    private String errorMessage;
    private String stdout;
    private String stderr;
    /** 本地脚本调试不调用模型，默认为 0；模型型调试可如实回传 Token 用量。 */
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}
