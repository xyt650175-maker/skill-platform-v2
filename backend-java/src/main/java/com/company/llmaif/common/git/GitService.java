package com.company.llmaif.common.git;

import com.company.llmaif.common.AgentException;
import com.company.llmaif.common.redis.RedisGitLock;
import com.company.llmaif.config.LlmaifProperties;
import com.company.llmaif.skills.dao.entity.SkillEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Skill Git 仓库服务。
 *
 * 每个 Skill 对应一个 NAS 上的 bare remote，以及一个位于草稿目录的工作副本。
 * 所有 JGit 写入均经此服务完成；暂存严格遵循“先 update、后 add”的规则。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitService {

    private final LlmaifProperties properties;
    private final RedisGitLock redisGitLock;

    /** 初始化 Skill 专属远端仓库和工作区，返回可展示的远端路径。 */
    public String initializeSkillRepository(SkillEntity skill) {
        return redisGitLock.execute("skill-" + skill.getId(), () -> initializeSkillRepositoryUnlocked(skill));
    }

    /**
     * 删除由平台为某个 Skill 创建的草稿工作区与 bare remote。
     * 仅用于删除 Skill 和创建过程失败后的补偿，避免残留名称占用或空壳仓库。
     */
    public void deleteSkillRepository(SkillEntity skill) {
        if (skill == null || skill.getId() == null) return;
        redisGitLock.execute("skill-" + skill.getId(), () -> {
            deleteTree(worktree(skill.getId()).getParent());
            deleteTree(remoteRoot().resolve("skill-" + skill.getId() + ".git").normalize());
            return null;
        });
    }

    /** 新建 Skill 前清理同 ID 的遗留本地目录（仅可能发生于本地 H2 重启后 ID 重用）。 */
    public void resetSkillRepository(SkillEntity skill) {
        if (skill == null || skill.getId() == null) return;
        redisGitLock.execute("skill-" + skill.getId(), () -> {
            deleteTree(worktree(skill.getId()).getParent());
            deleteTree(remoteRoot().resolve("skill-" + skill.getId() + ".git").normalize());
            return null;
        });
    }

    /** 将当前工作区全部变更提交并推送到 Skill 的 bare remote。 */
    public void commitWorkingTree(SkillEntity skill, String changedFile) {
        redisGitLock.execute("skill-" + skill.getId(), () -> {
            commitWorkingTreeUnlocked(skill, changedFile);
            return null;
        });
    }

    /** 每次保存均输出一个不可变草稿快照，供并发冲突回溯与定期清理。 */
    public String archiveDraft(SkillEntity skill) {
        return redisGitLock.execute("skill-" + skill.getId(), () -> {
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new java.util.Date());
            Path root = worktree(skill.getId());
            Path draftBase = Paths.get(properties.getNas().getSkillsDrafts()).toAbsolutePath().normalize();
            Path directory = draftBase.resolve(skill.getName()).resolve(timestamp).normalize();
            if (!directory.startsWith(draftBase)) throw new AgentException("非法 NAS 草稿路径");
            Files.createDirectories(directory);
            Path archive = directory.resolve(skill.getName() + ".zip");
            archiveDirectory(root, archive);
            return archive.toString();
        });
    }

    /** 企业内网 Git 的远程操作统一收敛在此服务，令牌不会跨过后端边界。 */
    public void verifyRemote(String repoUrl, String username, String token) throws Exception {
        // 某些企业 Git 网关只提供 dumb HTTP；JGit 的 ls-remote 在该协议下会空指针，
        // 但 clone 可正常工作。用浅克隆验证可同时覆盖真实读取权限。
        Path temp = Files.createTempDirectory("llmaif-git-verify-");
        try {
            cloneEnterpriseRepository(repoUrl, null, username, token, temp);
        } finally {
            deleteTree(temp);
        }
    }

    public java.util.Collection<Ref> listRemoteRefs(String repoUrl, String username, String token) throws Exception {
        Path temp = Files.createTempDirectory("llmaif-git-refs-");
        try {
            cloneEnterpriseRepository(repoUrl, null, username, token, temp);
            try (Git git = Git.open(temp.toFile())) {
                return new java.util.ArrayList<>(git.getRepository().getRefDatabase().getRefs());
            }
        } finally {
            deleteTree(temp);
        }
    }

    public void cloneEnterpriseRepository(String repoUrl, String ref, String username, String token, Path destination) throws Exception {
        org.eclipse.jgit.api.CloneCommand command = Git.cloneRepository().setURI(repoUrl).setDirectory(destination.toFile())
                .setCredentialsProvider(credentials(username, token));
        if (ref != null && !ref.trim().isEmpty()) command.setBranch(ref);
        try (Git ignored = command.call()) { }
    }

    private String initializeSkillRepositoryUnlocked(SkillEntity skill) {
        Path remote = remoteRoot().resolve("skill-" + skill.getId() + ".git").normalize();
        Path worktree = worktree(skill.getId());
        try {
            Files.createDirectories(remote.getParent());
            if (!Files.exists(remote.resolve("HEAD"))) {
                try (Git ignored = Git.init().setBare(true).setDirectory(remote.toFile()).call()) {
                    log.info("Created Skill bare repository: {}", remote);
                }
            }
            Files.createDirectories(worktree);
            if (!Files.exists(worktree.resolve(".git"))) {
                try (Git git = Git.init().setDirectory(worktree.toFile()).call()) {
                    StoredConfig config = git.getRepository().getConfig();
                    config.setString("remote", "origin", "url", remote.toUri().toString());
                    config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
                    config.save();
                }
            }
            return remote.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new AgentException("500", "初始化 Skill Git 仓库失败", e);
        }
    }


    private void commitWorkingTreeUnlocked(SkillEntity skill, String changedFile) {
        initializeSkillRepositoryUnlocked(skill);
        Path worktree = worktree(skill.getId());
        String branch = properties.getGit().getDefaultBranch();
        try (Git git = Git.open(worktree.toFile())) {
                // 空仓库的 HEAD 尚未解析，JGit 此时不能直接 checkout 新分支；
                // 先创建首个提交，再将默认 master 重命名为平台约定的 main。
                boolean hasCommit = git.getRepository().resolve(org.eclipse.jgit.lib.Constants.HEAD) != null;
                if (hasCommit) {
                    ensureBranch(git, branch);
                }
                // 规范要求：先暂存已有文件的修改/删除，再暂存新文件。
                git.add().setUpdate(true).addFilepattern(".").call();
                git.add().addFilepattern(".").call();
                git.commit()
                        .setMessage("chore(skill): save " + changedFile)
                        .setAllowEmpty(true)
                        .setAuthor("llmaif-platform", "platform@llmaif.local")
                        .setCommitter("llmaif-platform", "platform@llmaif.local")
                        .call();
                if (!hasCommit && !branch.equals(git.getRepository().getBranch())) {
                    git.branchRename().setNewName(branch).call();
                }
                git.push()
                        .setRemote("origin")
                        .setRefSpecs(new RefSpec("refs/heads/" + branch + ":refs/heads/" + branch))
                        .call();
        } catch (Exception e) {
            throw new AgentException("500", "提交 Skill Git 版本失败", e);
        }
    }

    /**
     * 将定版版本写入 SKILL.md 的 YAML 前言区，并为同一提交创建 Git Tag。
     * 前言区 version 与 Git Tag 使用同一版本号，保证发布产物可追溯。
     */
    public void finalizeVersion(SkillEntity skill, String version) {
        redisGitLock.execute("skill-" + skill.getId(), () -> {
            initializeSkillRepositoryUnlocked(skill);
            Path skillMd = worktree(skill.getId()).resolve("SKILL.md");
            try {
                String content = Files.exists(skillMd) ? new String(Files.readAllBytes(skillMd), java.nio.charset.StandardCharsets.UTF_8) : "";
                if (content.startsWith("---\n")) {
                    int closing = content.indexOf("\n---", 4);
                    if (closing < 0) {
                        throw new AgentException("500", "SKILL.md YAML 前言区不完整");
                    }
                    String frontmatter = content.substring(4, closing);
                    if (frontmatter.matches("(?s).*?(?m)^version:\\s*.*$.*")) {
                        frontmatter = frontmatter.replaceAll("(?m)^version:\\s*.*$", "version: " + version);
                    } else {
                        frontmatter = frontmatter + "\nversion: " + version;
                    }
                    content = "---\n" + frontmatter + content.substring(closing);
                } else {
                    // 兼容历史导入包：保留旧正文，并补齐符合当前规范的前言区。
                    content = "---\nname: " + skill.getName()
                            + "\nname_zh: " + skill.getName()
                            + "\ndescription: 历史导入 Skill，定版后请补充能力价值与触发场景说明。"
                            + "\nversion: " + version
                            + "\ntags: legacy\nrunEnv: all\ndigestValue: pending\n---\n\n"
                            + (content.isBlank() ? "# " + skill.getName() + "\n\n请补充 Skill 使用说明。\n" : content);
                }
                Files.write(skillMd, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                commitWorkingTreeUnlocked(skill, "SKILL.md（定版 " + version + "）");
                try (Git git = Git.open(worktree(skill.getId()).toFile())) {
                    String tag = String.format(com.company.llmaif.common.Constants.GIT_TAG_PATTERN, skill.getName(), version);
                    Ref existing = git.getRepository().findRef("refs/tags/" + tag);
                    if (existing == null) {
                        git.tag().setName(tag).setMessage("Skill " + skill.getName() + " " + version).call();
                        git.push().setRemote("origin").setPushTags().call();
                    }
                }
                archiveVersion(skill, version);
            } catch (Exception e) {
                throw new AgentException("500", "Skill 定版并同步 Git Tag 失败", e);
            }
            return null;
        });
    }

    private void ensureBranch(Git git, String branch) throws Exception {
        String current = git.getRepository().getBranch();
        if (!branch.equals(current)) {
            git.checkout().setCreateBranch(true).setName(branch).call();
        }
    }

    private Path remoteRoot() {
        return Paths.get(properties.getGit().getReposRoot()).toAbsolutePath().normalize();
    }

    private UsernamePasswordCredentialsProvider credentials(String username, String token) {
        return new UsernamePasswordCredentialsProvider(username == null ? "" : username, token);
    }

    private Path worktree(Long skillId) {
        return Paths.get(properties.getNas().getSkillsDrafts()).toAbsolutePath().normalize()
                .resolve("skill-" + skillId).resolve("current").normalize();
    }

    private void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    /** 生成符合 NAS 约定的定版 ZIP 包；当前版本保留由发布流程管理。 */
    private void archiveVersion(SkillEntity skill, String version) throws IOException {
        Path releaseDir = Paths.get(properties.getNas().getSkillsReleases()).toAbsolutePath().normalize()
                .resolve(skill.getName()).resolve(version).normalize();
        if (!releaseDir.startsWith(Paths.get(properties.getNas().getSkillsReleases()).toAbsolutePath().normalize())) {
            throw new AgentException("非法 NAS 版本路径");
        }
        Files.createDirectories(releaseDir);
        archiveDirectory(worktree(skill.getId()), releaseDir.resolve(skill.getName() + ".zip"));
    }

    private void archiveDirectory(Path root, Path zip) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip)); java.util.stream.Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile).filter(path -> !path.startsWith(root.resolve(".git"))).forEach(path -> {
                try {
                    output.putNextEntry(new ZipEntry(root.relativize(path).toString().replace('\\', '/')));
                    Files.copy(path, output);
                    output.closeEntry();
                } catch (IOException e) { throw new RuntimeException(e); }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) throw (IOException) e.getCause();
            throw e;
        }
    }
}
