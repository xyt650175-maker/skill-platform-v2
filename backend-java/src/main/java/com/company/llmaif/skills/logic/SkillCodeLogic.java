package com.company.llmaif.skills.logic;

import com.company.llmaif.common.AgentException;
import com.company.llmaif.common.git.GitService;
import com.company.llmaif.config.LlmaifProperties;
import com.company.llmaif.skills.dao.SkillDAO;
import com.company.llmaif.skills.dao.entity.SkillEntity;
import com.company.llmaif.skills.service.vo.SaveSkillCodeDTO;
import com.company.llmaif.skills.service.vo.SkillCodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Skill 草稿文件读写逻辑。
 * 草稿统一落在 NAS 的 skills/drafts 目录，路径始终由 Skill ID 派生，
 * 不接受客户端传入的绝对路径或上级目录，避免文件越权访问。
 */
@Component
@RequiredArgsConstructor
public class SkillCodeLogic {

    private static final long MAX_READ_BYTES = 2L * 1024L * 1024L;
    private static final Pattern YAML_FRONTMATTER = Pattern.compile("\\A---\\s*\\R([\\s\\S]*?)\\R---(?:\\s*\\R|\\z)");
    private static final Pattern VERSION_VALUE = Pattern.compile("(?m)^version:\\s*\\d+\\.\\d+\\.\\d+\\s*$");

    private final SkillDAO skillDAO;
    private final LlmaifProperties properties;
    private final GitService gitService;

    public SkillCodeVO getCode(Long skillId) {
        SkillEntity skill = getSkill(skillId);
        Path draftRoot = getDraftRoot(skill);
        SkillCodeVO result = new SkillCodeVO();
        result.setDraftRevision(skill.getDraftRevision() == null ? 0L : skill.getDraftRevision());

        if (!Files.exists(draftRoot)) {
            return result;
        }

        try (Stream<Path> paths = Files.walk(draftRoot)) {
            paths.filter(Files::isRegularFile)
                    // Git 元数据只能由 GitService 管理，绝不作为 Skill 文件返回给前端。
                    .filter(path -> !path.startsWith(draftRoot.resolve(".git")))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> result.getFiles().put(
                            draftRoot.relativize(path).toString().replace('\\', '/'),
                            readFile(path)));
        } catch (IOException e) {
            throw new AgentException("500", "读取 NAS 草稿文件失败", e);
        }

        String entryFile = skill.getEntryFile() == null ? "scripts/main.py" : skill.getEntryFile();
        result.setCode(result.getFiles().getOrDefault(entryFile, ""));
        result.setSkillsMd(result.getFiles().getOrDefault("SKILL.md", ""));
        return result;
    }

    public SkillCodeVO saveCode(Long skillId, SaveSkillCodeDTO dto) {
        SkillEntity skill = getSkill(skillId);
        long currentRevision = skill.getDraftRevision() == null ? 0L : skill.getDraftRevision();
        if (dto.getExpectedRevision() != null && !dto.getExpectedRevision().equals(currentRevision)) {
            throw new AgentException("草稿已被其他成员更新，请刷新后合并再保存");
        }
        validateSkillFile(dto.getEntryFile(), dto.getCode());
        Path target = resolveFile(getDraftRoot(skill), dto.getEntryFile());

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, dto.getCode().getBytes(StandardCharsets.UTF_8));
            refreshDigestValue(getDraftRoot(skill));
        } catch (IOException e) {
            throw new AgentException("500", "保存 NAS 草稿文件失败", e);
        }
        gitService.commitWorkingTree(skill, dto.getEntryFile());
        gitService.archiveDraft(skill);
        skill.setDraftRevision(currentRevision + 1);
        skillDAO.updateById(skill);
        return getCode(skillId);
    }

    private SkillEntity getSkill(Long skillId) {
        SkillEntity skill = skillDAO.selectById(skillId);
        if (skill == null) {
            throw new AgentException("Skill 不存在");
        }
        return skill;
    }

    private Path getDraftRoot(SkillEntity skill) {
        Path root = Paths.get(properties.getNas().getSkillsDrafts()).toAbsolutePath().normalize();
        return root.resolve("skill-" + skill.getId()).resolve("current").normalize();
    }

    private Path resolveFile(Path draftRoot, String entryFile) {
        if (entryFile == null || entryFile.contains(":")) {
            throw new AgentException("入口文件路径不合法");
        }
        final Path relative;
        try {
            relative = Paths.get(entryFile).normalize();
        } catch (Exception e) {
            throw new AgentException("入口文件路径不合法");
        }
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new AgentException("入口文件必须位于 Skill 草稿目录内");
        }
        Path target = draftRoot.resolve(relative).normalize();
        if (!target.startsWith(draftRoot)) {
            throw new AgentException("入口文件必须位于 Skill 草稿目录内");
        }
        return target;
    }

    /**
     * 《SKILL 开发规范》的服务端底线校验。前端提示词和预校验用于引导，
     * 此处保证绕过浏览器的接口调用也不能写入明显不合规的 Skill 文件。
     */
    private void validateSkillFile(String entryFile, String content) {
        if (entryFile == null || content == null || content.trim().isEmpty()) {
            throw new AgentException("Skill 文件路径与内容不能为空");
        }
        if ("README.md".equalsIgnoreCase(entryFile)) {
            throw new AgentException("SKILL 开发规范禁止使用 README.md，请使用 SKILL.md");
        }
        if (entryFile.toLowerCase().endsWith(".yaml") || entryFile.toLowerCase().endsWith(".yml")) {
            throw new AgentException("除 SKILL.md 顶部 YAML 前言区外，禁止创建独立 YAML 文件");
        }
        if (!("SKILL.md".equals(entryFile) || "requirements.txt".equals(entryFile)
                || entryFile.startsWith("scripts/") || entryFile.startsWith("references/") || entryFile.startsWith("assets/"))) {
            throw new AgentException("Skill 文件必须位于 SKILL.md、requirements.txt、scripts/、references/ 或 assets/ 中");
        }
        if (entryFile.toLowerCase().endsWith(".py")) {
            validatePythonSyntax(content);
        }
        if (!"SKILL.md".equals(entryFile)) {
            return;
        }
        if (content.split("\\R").length > 500) {
            throw new AgentException("SKILL.md 不得超过 500 行");
        }
        Matcher matcher = YAML_FRONTMATTER.matcher(content);
        if (!matcher.find()) {
            throw new AgentException("SKILL.md 必须以 YAML 前言区开始，并使用 --- 与正文分隔");
        }
        String frontmatter = matcher.group(1);
        requireFrontmatterField(frontmatter, "name");
        requireFrontmatterField(frontmatter, "name_zh");
        requireFrontmatterField(frontmatter, "description");
        requireFrontmatterField(frontmatter, "tags");
        requireFrontmatterField(frontmatter, "runEnv");
        requireFrontmatterField(frontmatter, "digestValue");
        if (!VERSION_VALUE.matcher(frontmatter).find()) {
            throw new AgentException("SKILL.md 前言区的 version 必须为 x.y.z 格式");
        }
        if (content.substring(matcher.end()).trim().isEmpty()) {
            throw new AgentException("SKILL.md 必须包含具体步骤或使用说明正文");
        }
    }

    /**
     * Python 脚本在写入草稿区前先完成语法校验。这样模型回答中混入 Markdown
     * 标记或自然语言说明时，会在保存阶段被拦截，而不是等到用户进入调试后才失败。
     */
    private void validatePythonSyntax(String content) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("llmaif-skill-syntax-", ".py");
            Files.write(temporaryFile, content.getBytes(StandardCharsets.UTF_8));
            Process process = new ProcessBuilder("python3", "-I", "-S", "-m", "py_compile", temporaryFile.toString())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AgentException("Python 脚本语法校验超时，请检查脚本内容后重试");
            }
            if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String reason = output.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .reduce((first, second) -> second)
                        .orElse("Python 语法错误");
                throw new AgentException("Python 脚本语法校验失败：" + reason);
            }
        } catch (IOException e) {
            throw new AgentException("500", "Python 脚本语法校验无法启动", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentException("Python 脚本语法校验被中断");
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // 临时文件会由系统清理，不能影响用户保存流程。
                }
            }
        }
    }

    private void requireFrontmatterField(String frontmatter, String field) {
        Pattern pattern = Pattern.compile("(?m)^" + Pattern.quote(field) + ":\\s*\\S+");
        if (!pattern.matcher(frontmatter).find()) {
            throw new AgentException("SKILL.md 前言区缺少 " + field);
        }
    }

    /**
     * 摘要由平台计算，避免模型或浏览器伪造。计算范围为当前 Skill 的全部业务文件，
     * 不含 .git 元数据和 SKILL.md 自身，防止自引用导致摘要永远变化。
     */
    private void refreshDigestValue(Path draftRoot) throws IOException {
        Path skillMd = draftRoot.resolve("SKILL.md");
        if (!Files.isRegularFile(skillMd)) return;
        String content = readFile(skillMd);
        Matcher matcher = YAML_FRONTMATTER.matcher(content);
        if (!matcher.find()) return;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            try (Stream<Path> files = Files.walk(draftRoot)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> !path.equals(skillMd))
                        .filter(path -> !path.startsWith(draftRoot.resolve(".git")))
                        .sorted(Comparator.comparing(path -> draftRoot.relativize(path).toString()))
                        .forEach(path -> updateDigest(md5, draftRoot, path));
            }
            StringBuilder digest = new StringBuilder();
            for (byte value : md5.digest()) digest.append(String.format("%02x", value));
            String updated = content.replaceFirst("(?m)^digestValue:\\s*.*$", "digestValue: " + digest);
            Files.write(skillMd, updated.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new AgentException("500", "当前运行环境不支持 MD5 摘要计算", e);
        }
    }

    private void updateDigest(MessageDigest md5, Path root, Path file) {
        try {
            md5.update(root.relativize(file).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
            md5.update((byte) 0);
            md5.update(Files.readAllBytes(file));
            md5.update((byte) 0);
        } catch (IOException e) {
            throw new AgentException("500", "计算 Skill 摘要失败", e);
        }
    }

    private String readFile(Path path) {
        try {
            if (Files.size(path) > MAX_READ_BYTES) {
                throw new AgentException("草稿文件超过 2MB，无法在编辑器中打开");
            }
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AgentException("500", "读取 NAS 草稿文件失败", e);
        }
    }
}
