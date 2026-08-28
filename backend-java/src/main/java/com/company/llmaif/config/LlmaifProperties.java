package com.company.llmaif.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "llmaif")
public class LlmaifProperties {

    private Jwt jwt = new Jwt();
    private Git git = new Git();
    private Nas nas = new Nas();
    private K8s k8s = new K8s();
    private Llm llm = new Llm();
    private Security security = new Security();

    @Data
    public static class Jwt {
        private String secret = "";
        private long expireMs = 86400000L;
        private String header = "Authorization";
        private String prefix = "Bearer ";
    }

    @Data
    public static class Git {
        private String reposRoot = "/agent_nas/git-repos";
        private String defaultBranch = "main";
        private int lockTimeoutSeconds = 30;
        private boolean redisLockEnabled = true;
        /** 企业内网 Git 服务地址前缀；为空时仅允许平台内部 NAS 仓库。 */
        private String enterpriseBaseUrl = "";
        /** 演示/本地联调可关闭真实连通性测试，生产环境必须开启。 */
        private boolean verifyRemote = true;
    }

    @Data
    public static class Nas {
        private String root = "/agent_nas";
        private String skillsDrafts = "/agent_nas/skills/drafts";
        private String skillsReleases = "/agent_nas/skills";
        private int ttlDays = 3;
    }

    @Data
    public static class K8s {
        private String namespace = "llmaif";
        private String kubeconfig = "";
    }

    @Data
    public static class Llm {
        private String baseUrl = "http://127.0.0.1:11434/v1";
        private String apiKey = "ollama";
        /** 本地 Ollama 等不需要令牌的 OpenAI 兼容服务可关闭此项。 */
        private boolean apiKeyRequired = false;
        private String defaultModel = "qwen2.5:3b";
        private int timeoutSeconds = 180;
    }

    @Data
    public static class Security {
        /** 从企业 KMS 同步或由 Kubernetes Secret 注入的凭证加密材料。 */
        private String credentialEncryptionKey = "";
    }
}
