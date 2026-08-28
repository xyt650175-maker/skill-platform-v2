-- 初始化数据库 schema
-- 数据库：llmaif（TDSQL，MySQL 兼容）

CREATE DATABASE IF NOT EXISTS llmaif DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE llmaif;

-- 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `display_name` VARCHAR(64) DEFAULT NULL COMMENT '显示名',
  `role` VARCHAR(32) NOT NULL DEFAULT 'viewer' COMMENT '角色：admin/developer/qa/viewer',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0 启用 1 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- Skill 表
DROP TABLE IF EXISTS `skill`;
CREATE TABLE `skill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL COMMENT 'Skill 名称',
  `description` VARCHAR(512) DEFAULT NULL,
  `runtime` VARCHAR(32) NOT NULL DEFAULT 'python' COMMENT '运行时',
  `git_repo_path` VARCHAR(512) DEFAULT NULL COMMENT 'Git 仓库路径',
  `current_version` VARCHAR(32) DEFAULT '0.0.0',
  `draft_revision` BIGINT NOT NULL DEFAULT 0 COMMENT '草稿修订号，用于并发覆盖检测',
  `status` VARCHAR(32) DEFAULT 'draft' COMMENT 'draft/testing/released',
  `visibility` VARCHAR(16) NOT NULL DEFAULT 'private' COMMENT '可见范围：public/private/team',
  `entry_file` VARCHAR(256) DEFAULT 'scripts/main.py',
  `creator_id` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 表';

-- 团队与成员关系：团队数据必须由企业身份/组织系统同步，所有团队级资源按此隔离。
CREATE TABLE `team` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(512) DEFAULT NULL,
  `owner_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队表';

CREATE TABLE `team_member` (
  `team_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `member_role` VARCHAR(32) NOT NULL DEFAULT 'member',
  PRIMARY KEY (`team_id`, `user_id`),
  KEY `idx_team_member_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员关系表';

CREATE TABLE `git_credential` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `team_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `repo_url` VARCHAR(512) NOT NULL,
  `auth_type` VARCHAR(32) NOT NULL,
  `username` VARCHAR(128) DEFAULT NULL,
  `secret_ciphertext` TEXT DEFAULT NULL COMMENT '仅保存 AES-GCM/KMS 密文',
  `creator_id` BIGINT NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_git_credential_team` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业内网 Git 凭证配置';

CREATE TABLE `skill_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `skill_id` BIGINT NOT NULL,
  `version` VARCHAR(32) NOT NULL,
  `source_type` VARCHAR(32) NOT NULL,
  `source_ref` VARCHAR(256) DEFAULT NULL,
  `change_summary` VARCHAR(512) DEFAULT NULL,
  `snapshot_path` VARCHAR(512) DEFAULT NULL,
  `creator_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_version` (`skill_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 版本与 NAS 快照索引';

CREATE TABLE `skill_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `skill_id` BIGINT NOT NULL,
  `version_id` BIGINT DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending',
  `comment` VARCHAR(1024) DEFAULT NULL,
  `applicant_id` BIGINT NOT NULL,
  `reviewer_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_skill_review_skill` (`skill_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 评审表';

-- Agent 表
DROP TABLE IF EXISTS `agent`;
CREATE TABLE `agent` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL COMMENT 'Agent 名称',
  `description` VARCHAR(512) DEFAULT NULL,
  `current_version` VARCHAR(32) DEFAULT '0.0.0',
  `status` VARCHAR(32) DEFAULT 'draft' COMMENT 'draft/active/deprecated',
  `model_name` VARCHAR(128) DEFAULT NULL COMMENT '模型名称',
  `system_prompt` TEXT DEFAULT NULL,
  `canvas_config` JSON DEFAULT NULL COMMENT 'DAG 画布配置',
  `creator_id` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 表';

-- 子 Agent 表
DROP TABLE IF EXISTS `sub_agent`;
CREATE TABLE `sub_agent` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agent_id` BIGINT NOT NULL COMMENT '所属 Agent',
  `name` VARCHAR(128) NOT NULL COMMENT '子 Agent 名称',
  `description` VARCHAR(512) DEFAULT NULL,
  `system_prompt` TEXT NOT NULL,
  `model_name` VARCHAR(128) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='子 Agent 表';

-- Skill 挂载表
DROP TABLE IF EXISTS `skill_mounting`;
CREATE TABLE `skill_mounting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agent_id` BIGINT NOT NULL COMMENT '所属 Agent',
  `sub_agent_id` BIGINT DEFAULT NULL COMMENT '挂载到子 Agent（可选）',
  `skill_id` BIGINT NOT NULL COMMENT 'Skill ID',
  `skill_alias` VARCHAR(128) DEFAULT NULL COMMENT 'Skill 别名',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 挂载表';

-- 评测任务表
DROP TABLE IF EXISTS `eval_task`;
CREATE TABLE `eval_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL COMMENT '评测任务名称',
  `agent_id` BIGINT NOT NULL COMMENT '评测的 Agent',
  `agent_version` VARCHAR(32) DEFAULT NULL COMMENT 'Agent 版本',
  `dataset_key` VARCHAR(64) NOT NULL COMMENT '测评集 key',
  `dataset_version` VARCHAR(32) DEFAULT NULL COMMENT '测评集版本',
  `scope` VARCHAR(32) DEFAULT 'all' COMMENT '执行范围：all/p0',
  `status` VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/running/completed/failed',
  `result_summary` JSON DEFAULT NULL COMMENT '评测结果汇总',
  `creator_id` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评测任务表';

-- Pipeline 表
DROP TABLE IF EXISTS `pipeline`;
CREATE TABLE `pipeline` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL COMMENT '流水线名称',
  `description` VARCHAR(512) DEFAULT NULL,
  `type` VARCHAR(32) DEFAULT 'factory' COMMENT '类型：factory/skill',
  `stages` JSON NOT NULL COMMENT '阶段配置',
  `current_version` VARCHAR(32) DEFAULT '0.0.0',
  `status` VARCHAR(32) DEFAULT 'draft' COMMENT 'draft/active/deprecated',
  `creator_id` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Pipeline 流水线表';

-- 初始化管理员账号（密码：admin123，生产环境请改为 BCrypt 加密）
INSERT INTO `sys_user` (`username`, `password`, `display_name`, `role`)
VALUES
  ('admin', 'admin123', '系统管理员', 'admin'),
  ('developer', 'dev123', 'Skill 开发者', 'developer'),
  ('qa', 'qa123', '测试工程师', 'qa');

-- 初始化仅用于最小可运行环境；生产环境请由企业组织同步服务维护团队与成员。
INSERT INTO `team` (`id`, `name`, `description`, `owner_id`) VALUES
  (1, '默认研发团队', '初始化团队', 1);
INSERT INTO `team_member` (`team_id`, `user_id`, `member_role`) VALUES
  (1, 1, 'admin'), (1, 2, 'member'), (1, 3, 'member');
