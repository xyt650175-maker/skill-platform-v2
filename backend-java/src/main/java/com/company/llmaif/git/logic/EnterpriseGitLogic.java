package com.company.llmaif.git.logic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.llmaif.common.AgentException;
import com.company.llmaif.common.git.GitService;
import com.company.llmaif.common.security.CredentialCipher;
import com.company.llmaif.config.LlmaifProperties;
import com.company.llmaif.git.dao.GitCredentialDAO;
import com.company.llmaif.git.dao.entity.GitCredentialEntity;
import com.company.llmaif.git.service.vo.GitCredentialDTO;
import com.company.llmaif.git.service.vo.GitCredentialVO;
import com.company.llmaif.git.service.vo.EnterpriseGitRefsVO;
import com.company.llmaif.git.service.vo.EnterpriseGitSkillVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 企业内网 Git 的凭证与连接校验；浏览器只取得脱敏后的连接信息。 */
@Component
@RequiredArgsConstructor
public class EnterpriseGitLogic {
    private static final long MAX_IMPORTED_FILE_BYTES = 2L * 1024L * 1024L;
    private final GitCredentialDAO credentialDAO;
    private final LlmaifProperties properties;
    private final CredentialCipher credentialCipher;
    private final GitService gitService;

    public List<GitCredentialVO> list(Long userId, Long teamId) {
        return credentialDAO.selectList(new LambdaQueryWrapper<GitCredentialEntity>()
                        .eq(GitCredentialEntity::getTeamId, teamId)
                        .orderByDesc(GitCredentialEntity::getUpdateTime))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public GitCredentialVO save(GitCredentialDTO dto, Long userId, Long teamId) {
        validateIntranetUrl(dto.getRepoUrl());
        GitCredentialEntity entity = dto.getId() == null ? new GitCredentialEntity() : credentialDAO.selectById(dto.getId());
        if (entity == null || (entity.getId() != null && !teamId.equals(entity.getTeamId()))) {
            throw new AgentException("Git 仓库连接配置不存在或无权限操作");
        }
        entity.setName(dto.getName().trim());
        entity.setRepoUrl(dto.getRepoUrl().trim());
        entity.setAuthType("personal-token");
        entity.setUsername(StringUtils.defaultString(dto.getUsername()).trim());
        entity.setCreatorId(userId);
        entity.setTeamId(teamId);
        if (StringUtils.isNotBlank(dto.getToken())) entity.setSecretCiphertext(credentialCipher.encrypt(dto.getToken()));
        if (StringUtils.isBlank(entity.getSecretCiphertext())) throw new AgentException("请填写企业 Git 访问令牌");
        if (entity.getId() == null) credentialDAO.insert(entity); else credentialDAO.updateById(entity);
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId, Long teamId) {
        GitCredentialEntity entity = credentialDAO.selectById(id);
        if (entity == null || !teamId.equals(entity.getTeamId())) throw new AgentException("Git 仓库连接配置不存在或无权限操作");
        credentialDAO.deleteById(id);
    }

    public GitCredentialVO test(Long id, Long userId, Long teamId) {
        GitCredentialEntity entity = requireCredential(id, teamId);
        if (properties.getGit().isVerifyRemote()) {
            try {
                gitService.verifyRemote(entity.getRepoUrl(), entity.getUsername(), credentialCipher.decrypt(entity.getSecretCiphertext()));
            } catch (Exception e) {
                throw new AgentException("企业 Git 连接失败：" + e.getMessage());
            }
        }
        GitCredentialVO vo = toVO(entity);
        vo.setConnectionStatus(properties.getGit().isVerifyRemote() ? "连接成功" : "本地联调：已校验内网配置，生产环境将由后端连通性验证");
        return vo;
    }

    /** 读取企业 Git 的 refs，供分支 / Tag 下拉框使用。 */
    public EnterpriseGitRefsVO listRefs(Long credentialId, Long userId, Long teamId) {
        GitCredentialEntity credential = requireCredential(credentialId, teamId);
        if (!properties.getGit().isVerifyRemote()) {
            throw new AgentException("本地联调未启用企业 Git 远程读取；请在部署环境开启 llmaif.git.verify-remote");
        }
        try {
            Collection<org.eclipse.jgit.lib.Ref> refs = gitService.listRemoteRefs(credential.getRepoUrl(), credential.getUsername(), credentialCipher.decrypt(credential.getSecretCiphertext()));
            EnterpriseGitRefsVO result = new EnterpriseGitRefsVO();
            refs.stream().map(org.eclipse.jgit.lib.Ref::getName).sorted().forEach(ref -> {
                if (ref.startsWith("refs/heads/")) result.getBranches().add(ref.substring("refs/heads/".length()));
                if (ref.startsWith("refs/tags/")) result.getTags().add(ref.substring("refs/tags/".length()));
            });
            return result;
        } catch (Exception e) {
            throw new AgentException("读取企业 Git 分支和 Tag 失败：" + e.getMessage());
        }
    }

    /**
     * 读取企业内网仓库指定分支或 Tag 中的标准 Skill 目录。
     * 只允许后端使用令牌 clone；前端仅收到可进入当前工作区的文本文件。
     */
    public List<EnterpriseGitSkillVO> loadSkills(Long credentialId, String ref, Long userId, Long teamId) {
        GitCredentialEntity credential = requireCredential(credentialId, teamId);
        if (!properties.getGit().isVerifyRemote()) {
            throw new AgentException("本地联调未启用企业 Git 远程读取；请在部署环境配置 ENTERPRISE_GIT_BASE_URL 并开启 llmaif.git.verify-remote");
        }
        if (StringUtils.isBlank(ref)) throw new AgentException("请选择要读取的分支或 Tag");

        Path tempRoot = Paths.get(properties.getNas().getRoot()).toAbsolutePath().normalize()
                .resolve("enterprise-git-import").resolve("credential-" + credentialId + "-" + System.nanoTime());
        try {
            Files.createDirectories(tempRoot.getParent());
            gitService.cloneEnterpriseRepository(credential.getRepoUrl(), ref, credential.getUsername(), credentialCipher.decrypt(credential.getSecretCiphertext()), tempRoot);
            try (Stream<Path> paths = Files.walk(tempRoot)) {
                return paths.filter(Files::isRegularFile)
                        .filter(path -> "SKILL.md".equals(path.getFileName().toString()))
                        .map(path -> readSkill(path.getParent(), tempRoot))
                        .collect(Collectors.toList());
            }
        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentException("读取企业 Git 仓库失败：" + e.getMessage());
        } finally {
            deleteTree(tempRoot);
        }
    }

    private EnterpriseGitSkillVO readSkill(Path skillRoot, Path cloneRoot) {
        try {
            Map<String, String> files = new LinkedHashMap<>();
            try (Stream<Path> paths = Files.walk(skillRoot)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> !path.startsWith(skillRoot.resolve(".git")))
                        .forEach(path -> {
                            String relative = skillRoot.relativize(path).toString().replace('\\', '/');
                            if (!("SKILL.md".equals(relative) || "requirements.txt".equals(relative)
                                    || relative.startsWith("scripts/") || relative.startsWith("references/") || relative.startsWith("assets/"))) return;
                            try {
                                if (Files.size(path) > MAX_IMPORTED_FILE_BYTES) {
                                    throw new AgentException("导入文件超过 2MB：" + relative);
                                }
                                files.put(relative, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
                            } catch (java.io.IOException e) {
                                throw new AgentException("读取导入文件失败：" + relative);
                            }
                        });
            }
            String skillMd = files.getOrDefault("SKILL.md", "");
            EnterpriseGitSkillVO vo = new EnterpriseGitSkillVO();
            vo.setPath(cloneRoot.relativize(skillRoot).toString().replace('\\', '/'));
            // 当前规范使用 YAML 前言区；同时兼容历史 Markdown 标题、版本和简介章节。
            vo.setName(markdownTitle(skillMd, firstMatch(skillMd, "(?m)^name:\\s*['\\\"]?([^\\n'\\\"]+)", skillRoot.getFileName().toString())));
            vo.setVersion(markdownVersion(skillMd, firstMatch(skillMd, "(?m)^version:\\s*['\\\"]?([0-9]+\\.[0-9]+\\.[0-9]+)", "0.0.0")));
            vo.setDescription(markdownDescription(skillMd, firstMatch(skillMd, "(?m)^description:\\s*['\\\"]?([^\\n'\\\"]+)", "从企业 Git 导入")));
            vo.setFiles(files);
            return vo;
        } catch (java.io.IOException | java.io.UncheckedIOException e) {
            throw new AgentException("读取企业 Git Skill 文件失败");
        }
    }

    private String firstMatch(String text, String expression, String fallback) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(expression).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : fallback;
    }

    private String markdownTitle(String text, String fallback) {
        return firstMatch(text, "(?m)^#\\s+([^#\\n]+?)\\s*$", fallback);
    }

    private String markdownVersion(String text, String fallback) {
        String version = firstMatch(text, "(?m)^\\s*[-*]\\s*当前版本：\\s*([0-9]+\\.[0-9]+\\.[0-9]+)\\s*$", "");
        return StringUtils.isNotBlank(version) ? version : fallback;
    }

    private String markdownDescription(String text, String fallback) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?ms)^##\\s*(?:简介|描述|用途)\\s*\\n+([^#]+?)(?=^##\\s|\\z)")
                .matcher(text);
        if (!matcher.find()) return fallback;
        String value = matcher.group(1).replaceAll("(?m)^\\s*[-*]\\s*", "").trim();
        return StringUtils.isNotBlank(value) ? value.replaceAll("\\s+", " ") : fallback;
    }

    private GitCredentialEntity requireCredential(Long id, Long teamId) {
        GitCredentialEntity entity = credentialDAO.selectById(id);
        if (entity == null || !teamId.equals(entity.getTeamId())) throw new AgentException("Git 仓库连接配置不存在或无权限操作");
        return entity;
    }

    private void deleteTree(Path root) {
        if (!Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (Exception ignored) { }
            });
        } catch (Exception ignored) { }
    }

    private void validateIntranetUrl(String repoUrl) {
        if (StringUtils.isBlank(repoUrl) || !(repoUrl.startsWith("https://") || repoUrl.startsWith("http://") || repoUrl.startsWith("ssh://"))) {
            throw new AgentException("仓库地址须为企业内网 HTTP(S) 或 SSH 地址");
        }
        String baseUrl = properties.getGit().getEnterpriseBaseUrl();
        if (StringUtils.isNotBlank(baseUrl) && !repoUrl.startsWith(baseUrl)) {
            throw new AgentException("仓库地址不属于当前配置的企业内网 Git 服务");
        }
    }

    private GitCredentialVO toVO(GitCredentialEntity entity) {
        GitCredentialVO vo = new GitCredentialVO();
        vo.setId(entity.getId()); vo.setName(entity.getName()); vo.setRepoUrl(entity.getRepoUrl());
        vo.setAuthType("企业访问令牌"); vo.setUsername(entity.getUsername()); vo.setTokenMasked("已由后端保管"); vo.setConnectionStatus("未验证");
        return vo;
    }
}
