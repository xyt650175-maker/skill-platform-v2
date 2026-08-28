package com.company.llmaif.skills.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.llmaif.skills.logic.SkillLogic;
import com.company.llmaif.skills.service.ISkillService;
import com.company.llmaif.skills.service.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Skill Service 实现
 */
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements ISkillService {

    private final SkillLogic skillLogic;

    @Override
    public SkillVO createSkill(CreateSkillDTO dto, Long creatorId) {
        return skillLogic.createSkill(dto, creatorId);
    }

    @Override
    public SkillVO updateSkill(UpdateSkillDTO dto) {
        return skillLogic.updateSkill(dto);
    }

    @Override
    public void deleteSkill(Long id) {
        skillLogic.deleteSkill(id);
    }

    @Override
    public SkillVO getSkillById(Long id) {
        return skillLogic.getSkillById(id);
    }

    @Override
    public List<SkillVO> listSkills(String status, Long creatorId) {
        return skillLogic.listSkills(status, creatorId);
    }

    @Override
    public void updateVersion(Long id, String newVersion) {
        skillLogic.updateVersion(id, newVersion);
    }

    @Override
    public SkillReviewVO submitReview(Long id, Long applicantId, java.util.Map<String, Object> debugResult) {
        return skillLogic.submitReview(id, applicantId, debugResult);
    }

    @Override
    public List<SkillReviewVO> listReviews(String status) {
        return skillLogic.listReviews(status);
    }

    @Override
    public SkillReviewVO decideReview(Long reviewId, SkillReviewDecisionDTO dto, Long reviewerId, String reviewerName) {
        return skillLogic.decideReview(reviewId, dto, reviewerId, reviewerName);
    }

    @Override
    public IPage<SkillOperationLogVO> listOperationLogs(Long skillId, int page, int size) {
        return skillLogic.listOperationLogs(skillId, page, size);
    }
}
