package com.company.llmaif.skills.logic;

import com.company.llmaif.skills.dao.SkillVersionDAO;
import com.company.llmaif.skills.dao.entity.SkillVersionEntity;
import com.company.llmaif.skills.service.vo.SkillVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Skill 版本逻辑
 */
@Component
@RequiredArgsConstructor
public class SkillVersionLogic {

    private final SkillVersionDAO skillVersionDAO;

    /**
     * 获取 Skill 版本列表
     */
    public List<SkillVersionVO> listVersions(Long skillId) {
        return skillVersionDAO.selectBySkillId(skillId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 创建版本快照记录
     */
    public SkillVersionVO createVersion(Long skillId, String version, String changeSummary, String snapshotPath, Long creatorId) {
        SkillVersionEntity entity = new SkillVersionEntity();
        entity.setSkillId(skillId);
        entity.setVersion(version);
        entity.setSourceType("release");
        entity.setChangeSummary(changeSummary);
        entity.setSnapshotPath(snapshotPath);
        entity.setCreatorId(creatorId);
        skillVersionDAO.insert(entity);
        return convertToVO(entity);
    }

    /**
     * 比较两个版本的差异
     */
    public Map<String, Object> diffVersions(String snapshotPath1, String snapshotPath2) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> fileDiffs = new ArrayList<>();

        try {
            Path dir1 = Paths.get(snapshotPath1);
            Path dir2 = Paths.get(snapshotPath2);

            if (!Files.exists(dir1) || !Files.exists(dir2)) {
                result.put("error", "版本快照目录不存在");
                return result;
            }

            // 获取所有文件（排除 .git 目录）
            Set<String> files1 = getFiles(dir1);
            Set<String> files2 = getFiles(dir2);

            Set<String> allFiles = new TreeSet<>();
            allFiles.addAll(files1);
            allFiles.addAll(files2);
            allFiles.removeIf(f -> f.startsWith(".git"));

            for (String file : allFiles) {
                Map<String, Object> fileDiff = new HashMap<>();
                fileDiff.put("path", file);

                Path path1 = dir1.resolve(file);
                Path path2 = dir2.resolve(file);

                boolean exists1 = Files.exists(path1);
                boolean exists2 = Files.exists(path2);

                if (exists1 && !exists2) {
                    fileDiff.put("status", "deleted");
                    fileDiff.put("content1", readFile(path1));
                } else if (!exists1 && exists2) {
                    fileDiff.put("status", "added");
                    fileDiff.put("content2", readFile(path2));
                } else {
                    String content1 = readFile(path1);
                    String content2 = readFile(path2);
                    if (!content1.equals(content2)) {
                        fileDiff.put("status", "modified");
                        fileDiff.put("content1", content1);
                        fileDiff.put("content2", content2);
                        fileDiff.put("diff", computeDiff(content1, content2));
                    } else {
                        fileDiff.put("status", "unchanged");
                    }
                }
                fileDiffs.add(fileDiff);
            }
        } catch (IOException e) {
            result.put("error", "读取版本快照失败: " + e.getMessage());
        }

        result.put("files", fileDiffs);
        return result;
    }

    private Set<String> getFiles(Path dir) throws IOException {
        Set<String> files = new HashSet<>();
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> files.add(dir.relativize(p).toString()));
        }
        return files;
    }

    private String readFile(Path path) throws IOException {
        return new String(Files.readAllBytes(path));
    }

    /**
     * 简单的行级别 diff
     */
    private List<Map<String, Object>> computeDiff(String content1, String content2) {
        List<Map<String, Object>> diff = new ArrayList<>();
        String[] lines1 = content1.split("\n", -1);
        String[] lines2 = content2.split("\n", -1);

        int maxLines = Math.max(lines1.length, lines2.length);
        for (int i = 0; i < maxLines; i++) {
            Map<String, Object> lineDiff = new HashMap<>();
            lineDiff.put("line", i + 1);

            String l1 = i < lines1.length ? lines1[i] : null;
            String l2 = i < lines2.length ? lines2[i] : null;

            if (l1 == null) {
                lineDiff.put("type", "added");
                lineDiff.put("content", l2);
            } else if (l2 == null) {
                lineDiff.put("type", "deleted");
                lineDiff.put("content", l1);
            } else if (!l1.equals(l2)) {
                lineDiff.put("type", "modified");
                lineDiff.put("oldContent", l1);
                lineDiff.put("newContent", l2);
            }
            if (l1 != null || l2 != null) {
                diff.add(lineDiff);
            }
        }
        return diff;
    }

    private SkillVersionVO convertToVO(SkillVersionEntity entity) {
        SkillVersionVO vo = new SkillVersionVO();
        vo.setId(entity.getId());
        vo.setSkillId(entity.getSkillId());
        vo.setVersion(entity.getVersion());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceRef(entity.getSourceRef());
        vo.setChangeSummary(entity.getChangeSummary());
        vo.setSnapshotPath(entity.getSnapshotPath());
        vo.setCreatorId(entity.getCreatorId());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
