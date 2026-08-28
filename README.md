# Skill Platform v2

企业内网场景下的 Skill 开发与管理平台。平台提供 Skill 创建、在线生成、文件编辑、受限调试、版本管理、智能体关联和企业 Git 连接管理能力。

## 项目结构

| 目录 | 说明 |
| --- | --- |
| `frontend-vite/` | Vue 3 + TypeScript + Vite 前端应用 |
| `backend-java/` | Spring Boot + MyBatis-Plus 后端服务 |
| `docs/` | 产品设计、工程架构、使用手册和测试报告 |
| `scripts/` | 本地验证脚本 |

## 主要能力

- **技能管理**：按范围管理个人与团队 Skill，查看状态、版本和基本信息。
- **技能开发**：在线创建、本地 ZIP 导入、企业 Git 导入；支持关联智能体及其版本。
- **AI 生成与迭代**：通过兼容 OpenAI 协议的模型服务生成或持续修改 `SKILL.md`、`requirements.txt`、`references/`、`scripts/` 中的文件。
- **调试与日志**：基于测试模板或业务测试数据运行单例/全量用例，展示输出、失败日志和 Token 消耗。
- **版本与 Git**：草稿保存、提交评审、版本历史、企业内网 Git 仓库连接与导入。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Pinia、Element Plus、CodeMirror。
- 后端：Java 8、Spring Boot 2.7、MyBatis-Plus、H2（本地联调）/ TDSQL MySQL（生产）、Redis、JGit。
- 模型服务：OpenAI 兼容接口；默认配置支持本机 Ollama，也可通过环境变量指向企业模型网关。

## 本地启动

### 1. 前端

```bash
cd frontend-vite
npm install
npm run dev
```

前端默认地址：<http://127.0.0.1:5177>

### 2. 后端

```bash
cd backend-java
mvn spring-boot:run
```

本地 Profile 默认使用内存 H2，服务地址：<http://127.0.0.1:8084/race-api>

前端已将 `/race-api` 代理至 `http://127.0.0.1:8084`。

### 3. 模型服务配置（可选）

模型配置仅从环境变量或本地未提交的 `application-local.yml` 读取，真实密钥不得写入仓库。

使用本机 Ollama：

```bash
export LOCAL_LLM_BASE_URL=http://127.0.0.1:11434/v1
export LOCAL_LLM_API_KEY=ollama
export LOCAL_LLM_MODEL=qwen2.5:3b
```

使用企业模型网关时，设置 `LOCAL_LLM_BASE_URL`、`LOCAL_LLM_API_KEY` 和 `LOCAL_LLM_MODEL` 后再启动后端。

## 验证

前端单元测试：

```bash
cd frontend-vite
npm run test:unit
```

前端类型检查与构建：

```bash
npm run typecheck
npm run build
```

## 文档

- [工程架构与编码规范](docs/工程架构与编码规范（已脱敏）.md)
- [技能产品设计说明](docs/技能产品设计说明.md)
- [平台使用手册](docs/平台使用手册.md)
- [Skill 开发规范](docs/SKILL开发规范.md)

## 安全说明

- `.gitignore` 已排除本地配置、运行时目录、构建产物、日志和环境变量文件。
- 企业 Git 访问令牌仅在服务端保存和使用，浏览器端不持久化令牌。
- 生产部署前应替换 JWT 密钥、凭证加密密钥、数据库连接和 NAS 挂载路径。
