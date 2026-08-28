package com.company.llmaif.skills.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.llmaif.skills.service.vo.*;

import java.util.List;

/**
 * Skill Service 接口
 */
public interface ISkillService {

    /**
     * 创建 Skill
     */
    SkillVO createSkill(CreateSkillDTO dto, Long creatorId);

    /**
     * 更新 Skill
     */
    SkillVO updateSkill(UpdateSkillDTO dto);

    /**
     * 删除 Skill
     */
    void deleteSkill(Long id);

    /**
     * 获取 Skill 详情
     */
    SkillVO getSkillById(Long id);

    /**
     * 查询 Skill 列表
     */
    List<SkillVO> listSkills(String status, Long creatorId);

    /**
     * 更新版本
     */
    void updateVersion(Long id, String newVersion);

    SkillReviewVO submitReview(Long id, Long applicantId, java.util.Map<String, Object> debugResult);

    List<SkillReviewVO> listReviews(String status);

    SkillReviewVO decideReview(Long reviewId, SkillReviewDecisionDTO dto, Long reviewerId, String reviewerName);

    IPage<SkillOperationLogVO> listOperationLogs(Long skillId, int page, int size);
}
