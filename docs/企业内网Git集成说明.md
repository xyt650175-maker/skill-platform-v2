# 企业内网 Git 集成说明

本项目遵循《工程架构与编码规范（已脱敏）》：Git 版本管理由 Spring Boot 后端的 `GitService`（JGit）统一执行，NAS 保存内部仓库、草稿与版本包；浏览器不直接连接 Git 服务，也不持久化访问令牌。

## 两类仓库

1. **平台内部版本仓库**：创建 Skill 时，后端在 NAS 挂载目录初始化独立 bare Git remote。草稿保存会提交到默认分支；定版会同步 `SKILL.md` 版本并创建 Git Tag。
2. **企业内网导入仓库**：用于“从企业 Git 导入”。业务人员配置企业 GitLab/Gitea 等内网仓库连接，平台后端用 JGit 在企业网络内读取分支、Tag 与 Skill 文件。

## 凭证数据流

```text
浏览器填写一次令牌
  → POST /enterprise-git/credentials
  → 后端保存至 git_credential.secret_ciphertext
  → 前端仅取得“已由后端保管”
  → 后端 JGit 测试/读取企业内网仓库
```

令牌不写入 `localStorage`、Skill 文件、NAS 草稿或浏览器日志。生产环境需将 `secret_ciphertext` 对接企业密钥管理服务或数据库字段加密；本地联调仅用于验证接口链路，不能作为生产密钥方案。

## 部署配置

在生产环境设置企业内网 Git 域名，不配置公网地址：

```yaml
llmaif:
  git:
    enterprise-base-url: https://git.intra.example.com
    verify-remote: true
    repos-root: /agent_nas/git-repos
```

本地副本中 `verify-remote: false`，因此“测试连接”只完成内网地址与后端保存链路校验，不产生任何外部网络访问。

## 示例

```text
连接名称：业务团队 Skill 仓库
仓库地址：https://git.intra.example.com/internal/skills.git
用户名：skill-service-bot
访问令牌：由企业内网 Git 平台签发，授予 read_repository；需要推送定版时授予 write_repository。
```
