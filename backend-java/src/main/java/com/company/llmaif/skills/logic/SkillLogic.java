package com.company.llmaif.skills.logic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.llmaif.common.AgentException;
import com.company.llmaif.common.git.GitService;
import com.company.llmaif.config.LlmaifProperties;
import com.company.llmaif.skills.dao.SkillDAO;
import com.company.llmaif.skills.dao.entity.SkillEntity;
import com.company.llmaif.skills.dao.SkillReviewDAO;
import com.company.llmaif.skills.dao.entity.SkillReviewEntity;
import com.company.llmaif.skills.enums.SkillStatusEnum;
import com.company.llmaif.skills.service.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Skill 业务逻辑层
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillLogic {

    private final SkillDAO skillDAO;
    private final SkillReviewDAO skillReviewDAO;
    private final GitService gitService;
    private final OperationLogLogic operationLogLogic;
    private final SkillVersionLogic skillVersionLogic;
    private final LlmaifProperties properties;

    /**
     * 创建 Skill
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillVO createSkill(CreateSkillDTO dto, Long creatorId) {
        // 检查名称是否已存在
        SkillEntity existing = skillDAO.selectByName(dto.getName());
        if (existing != null) {
            throw new AgentException("Skill 名称已存在");
        }

        // 构建实体
        SkillEntity entity = new SkillEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setRuntime(dto.getRuntime() != null ? dto.getRuntime() : "python");
        entity.setGitRepoPath(dto.getGitRepoPath());
        entity.setEntryFile(dto.getEntryFile() != null ? dto.getEntryFile() : "scripts/main.py");
        entity.setCurrentVersion(StringUtils.defaultIfBlank(dto.getVersion(), "0.0.0"));
        entity.setDraftRevision(0L);
        entity.setStatus(SkillStatusEnum.DRAFT.getCode());
        // Skill 不允许公开；仅可创建为私有或团队可见，服务端同样兜底防止绕过前端。
        entity.setVisibility("team".equals(dto.getVisibility()) ? "team" : "private");
        entity.setCreatorId(creatorId);

        skillDAO.insert(entity);
        try {
            // 创建独立 Git remote 与空工作区。任何初始化失败都会回滚数据库记录，
            // 不会留下“名称已存在”的空壳 Skill。
            gitService.resetSkillRepository(entity);
            entity.setGitRepoPath(gitService.initializeSkillRepository(entity));
            skillDAO.updateById(entity);
        } catch (RuntimeException e) {
            gitService.deleteSkillRepository(entity);
            throw e;
        }

        return convertToVO(entity);
    }

    /**
     * 更新 Skill
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillVO updateSkill(UpdateSkillDTO dto) {
        SkillEntity entity = skillDAO.selectById(dto.getId());
        if (entity == null) {
            throw new AgentException("Skill 不存在");
        }

        if (StringUtils.isNotBlank(dto.getDescription())) {
            entity.setDescription(dto.getDescription());
        }
        if (StringUtils.isNotBlank(dto.getRuntime())) {
            entity.setRuntime(dto.getRuntime());
        }
        if (StringUtils.isNotBlank(dto.getGitRepoPath())) {
            entity.setGitRepoPath(dto.getGitRepoPath());
        }
        if (StringUtils.isNotBlank(dto.getEntryFile())) {
            entity.setEntryFile(dto.getEntryFile());
        }
        if (StringUtils.isNotBlank(dto.getStatus())) {
            // 验证状态转换是否合法
            validateStatusTransition(entity.getStatus(), dto.getStatus());
            entity.setStatus(dto.getStatus());
        }

        skillDAO.updateById(entity);

        return convertToVO(entity);
    }

    /**
     * 删除 Skill
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkill(Long id) {
        SkillEntity entity = skillDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("Skill 不存在");
        }
        gitService.deleteSkillRepository(entity);
        skillDAO.deleteById(id);
    }

    /**
     * 获取 Skill 详情
     */
    public SkillVO getSkillById(Long id) {
        SkillEntity entity = skillDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("Skill 不存在");
        }
        return convertToVO(entity);
    }

    /**
     * 查询 Skill 列表
     */
    public List<SkillVO> listSkills(String status, Long creatorId) {
        LambdaQueryWrapper<SkillEntity> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(SkillEntity::getStatus, status);
        }
        if (creatorId != null) {
            wrapper.eq(SkillEntity::getCreatorId, creatorId);
        }

        wrapper.orderByDesc(SkillEntity::getCreateTime);

        List<SkillEntity> entities = skillDAO.selectList(wrapper);

        return entities.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 更新 Skill 版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateVersion(Long id, String newVersion) {
        SkillEntity entity = skillDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("Skill 不存在");
        }
        if (!newVersion.matches("^\\d+\\.\\d+\\.\\d+$")) {
            throw new AgentException("版本必须符合 x.y.z 格式");
        }
        gitService.finalizeVersion(entity, newVersion);
        entity.setCurrentVersion(newVersion);
        skillDAO.updateById(entity);
    }

    /** 提交评审仅冻结当前草稿的评审状态，不能提前产生发布版本。 */
    @Transactional(rollbackFor = Exception.class)
    public SkillReviewVO submitReview(Long id, Long applicantId, java.util.Map<String, Object> debugResult) {
        SkillEntity skill = skillDAO.selectById(id);
        if (skill == null) throw new AgentException("Skill 不存在");
        String status = skill.getStatus();
        if (!SkillStatusEnum.DRAFT.getCode().equals(status) && !"reviewing".equals(status) && !"rejected".equals(status)) {
            throw new AgentException("仅草稿、评审中或已驳回状态的 Skill 可以提交评审");
        }
        // 如果已有 pending 评审，先将其关闭
        Long pending = skillReviewDAO.selectCount(new LambdaQueryWrapper<SkillReviewEntity>()
                .eq(SkillReviewEntity::getSkillId, id).eq(SkillReviewEntity::getStatus, "pending"));
        if (pending > 0) {
            // 关闭旧的 pending 评审
            List<SkillReviewEntity> oldReviews = skillReviewDAO.selectList(new LambdaQueryWrapper<SkillReviewEntity>()
                    .eq(SkillReviewEntity::getSkillId, id).eq(SkillReviewEntity::getStatus, "pending"));
            for (SkillReviewEntity old : oldReviews) {
                old.setStatus("rejected");
                old.setComment("重新提交评审，自动关闭");
                skillReviewDAO.updateById(old);
            }
        }

        SkillReviewEntity review = new SkillReviewEntity();
        review.setSkillId(id); review.setStatus("pending"); review.setApplicantId(applicantId);
        review.setComment("申请评审版本 " + skill.getCurrentVersion());

        // 保存调试结果
        if (debugResult != null) {
            review.setDebugSummary((String) debugResult.get("debugSummary"));
            review.setDebugPassed(debugResult.get("debugPassed") != null ? (Boolean) debugResult.get("debugPassed") : null);
            review.setDebugInput((String) debugResult.get("debugInput"));
            review.setTotalTokens(debugResult.get("totalTokens") != null ? ((Number) debugResult.get("totalTokens")).intValue() : null);
            review.setTestCaseCount(debugResult.get("testCaseCount") != null ? ((Number) debugResult.get("testCaseCount")).intValue() : null);
            review.setTestPassCount(debugResult.get("testPassCount") != null ? ((Number) debugResult.get("testPassCount")).intValue() : null);
            review.setDebugResult(debugResult.get("debugResult") != null ? debugResult.get("debugResult").toString() : null);
        }

        skillReviewDAO.insert(review);
        skill.setStatus(SkillStatusEnum.REVIEWING.getCode());
        skillDAO.updateById(skill);

        String changeSummary = review.getDebugSummary() != null ? review.getDebugSummary() : "无调试结果";
        operationLogLogic.log(id, applicantId, "开发者", "submit_review",
                "提交v" + skill.getCurrentVersion() + "评审",
                changeSummary, "success", skill.getCurrentVersion());

        return toReviewVO(review, skill);
    }

    public List<SkillReviewVO> listReviews(String status) {
        LambdaQueryWrapper<SkillReviewEntity> wrapper = new LambdaQueryWrapper<SkillReviewEntity>().orderByDesc(SkillReviewEntity::getCreateTime);
        if (StringUtils.isNotBlank(status)) wrapper.eq(SkillReviewEntity::getStatus, status);
        return skillReviewDAO.selectList(wrapper).stream()
                .map(review -> toReviewVO(review, skillDAO.selectById(review.getSkillId())))
                .collect(Collectors.toList());
    }

    /** 评审通过的这一刻才将当前文件定版到 Git；驳回则保留草稿以便继续修改。 */
    @Transactional(rollbackFor = Exception.class)
    public SkillReviewVO decideReview(Long reviewId, SkillReviewDecisionDTO dto, Long reviewerId, String reviewerName) {
        SkillReviewEntity review = skillReviewDAO.selectById(reviewId);
        if (review == null) throw new AgentException("评审记录不存在");
        if (!"pending".equals(review.getStatus())) throw new AgentException("该评审已处理，不能重复操作");
        SkillEntity skill = skillDAO.selectById(review.getSkillId());
        if (skill == null) throw new AgentException("评审关联的 Skill 不存在");
        String decision = dto.getDecision().trim().toLowerCase();
        if (!"approved".equals(decision) && !"rejected".equals(decision)) throw new AgentException("评审结论仅支持 approved 或 rejected");

        review.setStatus(decision); review.setReviewerId(reviewerId); review.setComment(StringUtils.defaultIfBlank(dto.getComment(), review.getComment()));
        if ("approved".equals(decision)) {
            gitService.finalizeVersion(skill, skill.getCurrentVersion());
            skill.setStatus(SkillStatusEnum.RELEASED.getCode());

            // 创建版本快照：将草稿目录文件复制到发布目录
            String releasesRoot = properties.getNas().getSkillsReleases();
            String snapshotPath = releasesRoot + "/skill-" + skill.getId() + "/v" + skill.getCurrentVersion();
            createSnapshotDirectory(skill.getId(), snapshotPath);

            skillVersionLogic.createVersion(skill.getId(), skill.getCurrentVersion(),
                    StringUtils.defaultIfBlank(dto.getComment(), "评审通过定版"),
                    snapshotPath, reviewerId);

            operationLogLogic.log(skill.getId(), reviewerId, reviewerName, "approve",
                    "通过评审", StringUtils.defaultIfBlank(dto.getComment(), "通过"),
                    "success", skill.getCurrentVersion());
            operationLogLogic.log(skill.getId(), reviewerId, reviewerName, "publish",
                    "定版v" + skill.getCurrentVersion(),
                    "版本: " + skill.getCurrentVersion(),
                    "success", skill.getCurrentVersion());
        } else {
            skill.setStatus(SkillStatusEnum.DRAFT.getCode());
            operationLogLogic.log(skill.getId(), reviewerId, reviewerName, "reject",
                    "驳回评审", StringUtils.defaultIfBlank(dto.getComment(), "驳回"),
                    "success", skill.getCurrentVersion());
        }
        skillReviewDAO.updateById(review);
        skillDAO.updateById(skill);
        return toReviewVO(review, skill);
    }

    /**
     * 获取 Skill 操作日志
     */
    public com.baomidou.mybatisplus.core.metadata.IPage<SkillOperationLogVO> listOperationLogs(Long skillId, int page, int size) {
        return operationLogLogic.listLogs(skillId, page, size);
    }

    /**
     * 创建版本快照目录：将草稿目录下的所有文件复制到发布快照目录
     */
    private void createSnapshotDirectory(Long skillId, String snapshotPath) {
        try {
            String draftsRoot = properties.getNas().getSkillsDrafts();
            java.nio.file.Path draftDir = java.nio.file.Paths.get(draftsRoot, "skill-" + skillId, "current");
            java.nio.file.Path releaseDir = java.nio.file.Paths.get(snapshotPath);

            if (!java.nio.file.Files.exists(draftDir)) {
                log.warn("草稿目录不存在，跳过快照: {}", draftDir);
                return;
            }

            // 创建快照目录
            java.nio.file.Files.createDirectories(releaseDir);

            // 递归复制草稿目录下所有文件（排除 .git 目录）
            java.nio.file.Files.walk(draftDir)
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(source -> !draftDir.relativize(source).startsWith(".git"))
                    .forEach(source -> {
                        java.nio.file.Path relative = draftDir.relativize(source);
                        java.nio.file.Path target = releaseDir.resolve(relative);
                        try {
                            java.nio.file.Files.createDirectories(target.getParent());
                            java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (java.io.IOException e) {
                            log.error("复制快照文件失败: {} -> {}", source, target, e);
                        }
                    });

            log.info("版本快照已创建: {}", snapshotPath);
        } catch (Exception e) {
            log.error("创建版本快照失败", e);
        }
    }

    /**
     * 验证状态转换
     */
    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus.equals(newStatus)) {
            return;
        }

        // 正常发布链路：草稿 -> 评审中 -> 已发布。testing 保留给历史数据兼容。
        if (SkillStatusEnum.DRAFT.getCode().equals(currentStatus)) {
            if (!SkillStatusEnum.REVIEWING.getCode().equals(newStatus) && !SkillStatusEnum.TESTING.getCode().equals(newStatus)) {
                throw new AgentException("草稿状态只能提交评审或转为测试中");
            }
        } else if (SkillStatusEnum.REVIEWING.getCode().equals(currentStatus)) {
            if (!SkillStatusEnum.RELEASED.getCode().equals(newStatus) && !SkillStatusEnum.DRAFT.getCode().equals(newStatus)) {
                throw new AgentException("评审中状态只能转为已发布或草稿");
            }
        } else if (SkillStatusEnum.TESTING.getCode().equals(currentStatus)) {
            if (!SkillStatusEnum.RELEASED.getCode().equals(newStatus) &&
                    !SkillStatusEnum.DRAFT.getCode().equals(newStatus)) {
                throw new AgentException("测试中状态只能转为已发布或草稿");
            }
        } else if (SkillStatusEnum.RELEASED.getCode().equals(currentStatus)) {
            // 已发布的 Skill 可以退回草稿，用于创建新版本
            if (!SkillStatusEnum.DRAFT.getCode().equals(newStatus)) {
                throw new AgentException("已发布状态只能退回草稿以创建新版本");
            }
        }
    }

    /**
     * 转换为 VO
     */
    private SkillVO convertToVO(SkillEntity entity) {
        SkillVO vo = new SkillVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setRuntime(entity.getRuntime());
        vo.setGitRepoPath(entity.getGitRepoPath());
        vo.setCurrentVersion(entity.getCurrentVersion());
        vo.setStatus(entity.getStatus());
        vo.setVisibility(StringUtils.defaultIfBlank(entity.getVisibility(), "private"));
        vo.setEntryFile(entity.getEntryFile());
        vo.setCreatorId(entity.getCreatorId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        // 状态描述
        SkillStatusEnum statusEnum = SkillStatusEnum.fromCode(entity.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        return vo;
    }

    private SkillReviewVO toReviewVO(SkillReviewEntity review, SkillEntity skill) {
        SkillReviewVO vo = new SkillReviewVO();
        vo.setId(review.getId()); vo.setSkillId(review.getSkillId());
        vo.setSkillName(skill == null ? "已删除 Skill" : skill.getName());
        vo.setVersion(skill == null ? null : skill.getCurrentVersion());
        vo.setStatus(review.getStatus()); vo.setComment(review.getComment());
        vo.setApplicantId(review.getApplicantId()); vo.setReviewerId(review.getReviewerId());
        vo.setDebugResult(review.getDebugResult());
        vo.setDebugPassed(review.getDebugPassed());
        vo.setDebugInput(review.getDebugInput());
        vo.setTotalTokens(review.getTotalTokens());
        vo.setTestCaseCount(review.getTestCaseCount());
        vo.setTestPassCount(review.getTestPassCount());
        vo.setDebugSummary(review.getDebugSummary());
        vo.setCreateTime(review.getCreateTime()); vo.setUpdateTime(review.getUpdateTime());
        return vo;
    }
}
