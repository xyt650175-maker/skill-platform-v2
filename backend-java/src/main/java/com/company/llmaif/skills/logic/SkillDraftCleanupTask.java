package com.company.llmaif.skills.logic;

import com.company.llmaif.config.LlmaifProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

/** 清理超过 TTL 的草稿快照；当前工作区、Git remote、提交及定版 Tag 均会保留。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillDraftCleanupTask {
    private final LlmaifProperties properties;

    @Scheduled(fixedDelayString = "${llmaif.nas.draft-cleanup-delay-ms:43200000}")
    public void cleanupExpiredDrafts() {
        Path root = Paths.get(properties.getNas().getSkillsDrafts()).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) return;
        Instant deadline = Instant.now().minus(Duration.ofDays(Math.max(1, properties.getNas().getTtlDays())));
        try (Stream<Path> skillRoots = Files.list(root)) {
            skillRoots.filter(Files::isDirectory).forEach(skillRoot -> {
                if ("skill-".equals(skillRoot.getFileName().toString().substring(0, Math.min(6, skillRoot.getFileName().toString().length())))) return;
                try (Stream<Path> snapshots = Files.list(skillRoot)) {
                    snapshots.filter(Files::isDirectory).forEach(snapshot -> removeIfExpired(snapshot, deadline));
                } catch (IOException e) { log.warn("Unable to scan draft snapshots {}: {}", skillRoot, e.getMessage()); }
            });
        } catch (IOException e) { log.warn("Unable to scan expired Skill drafts: {}", e.getMessage()); }
    }

    private void removeIfExpired(Path skillRoot, Instant deadline) {
        Path worktree = skillRoot.normalize();
        if (!Files.isDirectory(worktree)) return;
        try {
            FileTime modified = Files.getLastModifiedTime(worktree);
            if (modified.toInstant().isAfter(deadline)) return;
            try (Stream<Path> files = Files.walk(worktree)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
            }
            log.info("Expired Skill draft worktree removed: {}", worktree);
        } catch (IOException e) { log.warn("Unable to clean expired Skill draft {}: {}", skillRoot, e.getMessage()); }
    }
}
