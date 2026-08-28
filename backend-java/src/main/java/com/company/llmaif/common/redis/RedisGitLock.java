package com.company.llmaif.common.redis;

import com.company.llmaif.common.AgentException;
import com.company.llmaif.common.Constants;
import com.company.llmaif.config.LlmaifProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Git 仓库级分布式锁。生产环境使用 Redis，锁键严格遵循 git:repo:{repoHash}。
 * 本地 profile 可显式关闭，避免本地原型在未安装 Redis 时伪装成分布式环境。
 */
@Component
@RequiredArgsConstructor
public class RedisGitLock {
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;
    private final LlmaifProperties properties;

    public <T> T execute(String repositoryIdentity, Callable<T> action) {
        if (!properties.getGit().isRedisLockEnabled()) return call(action);
        String key = Constants.GIT_LOCK_PREFIX + sha256(repositoryIdentity);
        String token = UUID.randomUUID().toString();
        Boolean locked;
        try {
            locked = redisTemplate.opsForValue().setIfAbsent(key, token,
                    properties.getGit().getLockTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AgentException("500", "Redis Git 锁不可用，已拒绝执行仓库写操作", e);
        }
        if (!Boolean.TRUE.equals(locked)) throw new AgentException("该 Skill 正在被其他成员编辑，请稍后重试");
        try {
            return call(action);
        } finally {
            try { redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token); }
            catch (Exception ignored) { /* TTL 兜底释放 */ }
        }
    }

    private <T> T call(Callable<T> action) {
        try { return action.call(); }
        catch (AgentException e) { throw e; }
        catch (Exception e) { throw new AgentException("500", "Git 操作失败", e); }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception e) { throw new AgentException("500", "生成 Git 锁标识失败", e); }
    }
}
