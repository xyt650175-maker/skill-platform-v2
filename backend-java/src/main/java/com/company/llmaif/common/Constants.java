package com.company.llmaif.common;

import java.io.Serializable;

/**
 * 通用常量
 */
public interface Constants extends Serializable {

    /** Git 锁 key 前缀，格式：git:repo:{repoHash} */
    String GIT_LOCK_PREFIX = "git:repo:";

    /** Git 锁超时（秒） */
    int GIT_LOCK_TIMEOUT_SECONDS = 30;

    /** 逻辑删除：未删除 */
    int NOT_DELETED = 0;

    /** 逻辑删除：已删除 */
    int DELETED = 1;

    /** 测试版本号格式 x.y.z */
    String VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+$";

    /** 草稿版本标识（路径用） */
    String DRAFT_PATH_PATTERN = "yyyyMMdd_HHmmss";

    /** Git Tag 格式：{skill-name}-{x.y.z}（无 v 前缀） */
    String GIT_TAG_PATTERN = "%s-%s";

    /** NAS 草稿路径 */
    String NAS_DRAFT_PATH = "/agent_nas/skills/drafts/%s/%s/%s.zip";

    /** NAS 测试版本路径 */
    String NAS_RELEASE_PATH = "/agent_nas/skills/%s/%s/%s.zip";

    /** NAS 版本保留 TTL（天） */
    int NAS_TTL_DAYS = 3;
}
