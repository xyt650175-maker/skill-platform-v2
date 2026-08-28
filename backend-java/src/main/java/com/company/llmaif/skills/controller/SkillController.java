package com.company.llmaif.skills.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.llmaif.common.ResponseBase;
import com.company.llmaif.skills.dao.SkillVersionDAO;
import com.company.llmaif.skills.dao.entity.SkillVersionEntity;
import com.company.llmaif.skills.logic.OperationLogLogic;
import com.company.llmaif.skills.logic.SkillVersionLogic;
import com.company.llmaif.skills.service.ISkillService;
import com.company.llmaif.skills.service.ISkillCodeService;
import com.company.llmaif.skills.service.ISkillDebugService;
import com.company.llmaif.skills.service.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Skill 控制器
 */
@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillController {

    private final ISkillService skillService;
    private final ISkillCodeService skillCodeService;
    private final ISkillDebugService skillDebugService;
    private final SkillVersionLogic skillVersionLogic;
    private final SkillVersionDAO skillVersionDAO;

    /**
     * 创建 Skill
     */
    @PostMapping
    public ResponseBase<SkillVO> create(@Validated @RequestBody CreateSkillDTO dto,
                                         @RequestAttribute("userId") Long userId) {
        SkillVO vo = skillService.createSkill(dto, userId);
        return ResponseBase.success(vo);
    }

    /**
     * 更新 Skill
     */
    @PutMapping
    public ResponseBase<SkillVO> update(@Validated @RequestBody UpdateSkillDTO dto) {
        SkillVO vo = skillService.updateSkill(dto);
        return ResponseBase.success(vo);
    }

    /**
     * 删除 Skill
     */
    @DeleteMapping("/{id}")
    public ResponseBase<Void> delete(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseBase.success(null);
    }

    /**
     * 获取 Skill 详情
     */
    @GetMapping("/{id}")
    public ResponseBase<SkillVO> getById(@PathVariable Long id) {
        SkillVO vo = skillService.getSkillById(id);
        return ResponseBase.success(vo);
    }

    /**
     * 查询 Skill 列表
     */
    @GetMapping
    public ResponseBase<List<SkillVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long creatorId) {
        List<SkillVO> list = skillService.listSkills(status, creatorId);
        return ResponseBase.success(list);
    }

    /**
     * 更新版本
     */
    @PutMapping("/{id}/version")
    public ResponseBase<Void> updateVersion(@PathVariable Long id,
                                             @RequestParam String version) {
        skillService.updateVersion(id, version);
        return ResponseBase.success(null);
    }

    /** 获取 NAS 中的当前草稿文件。 */
    @GetMapping("/{id}/code")
    public ResponseBase<SkillCodeVO> getCode(@PathVariable Long id) {
        return ResponseBase.success(skillCodeService.getCode(id));
    }

    /** 保存当前草稿入口文件到 NAS。 */
    @PostMapping("/{id}/code")
    public ResponseBase<SkillCodeVO> saveCode(@PathVariable Long id,
                                               @Validated @RequestBody SaveSkillCodeDTO dto) {
        return ResponseBase.success(skillCodeService.saveCode(id, dto));
    }

    /** 在受限本地运行器中调试当前草稿。该运行器不是容器级安全沙箱。 */
    @PostMapping("/{id}/debug")
    public ResponseBase<SkillDebugVO> debug(@PathVariable Long id,
                                             @Validated @RequestBody RunSkillDebugDTO dto) {
        return ResponseBase.success(skillDebugService.run(id, dto));
    }

    /** 提交评审，附带调试结果。 */
    @PostMapping("/{id}/reviews")
    public ResponseBase<SkillReviewVO> submitReview(@PathVariable Long id,
                                                     @RequestAttribute("userId") Long userId,
                                                     @RequestBody(required = false) Map<String, Object> debugResult) {
        return ResponseBase.success(skillService.submitReview(id, userId, debugResult));
    }

    @GetMapping("/reviews")
    public ResponseBase<List<SkillReviewVO>> listReviews(@RequestParam(required = false) String status) {
        return ResponseBase.success(skillService.listReviews(status));
    }

    /** 通过时才定版、打 tag 并生成发布包；驳回则退回草稿。 */
    @PutMapping("/reviews/{reviewId}")
    public ResponseBase<SkillReviewVO> decideReview(@PathVariable Long reviewId,
                                                     @Validated @RequestBody SkillReviewDecisionDTO dto,
                                                     @RequestAttribute("userId") Long userId) {
        return ResponseBase.success(skillService.decideReview(reviewId, dto, userId, "审核人"));
    }

    /** 获取 Skill 操作日志 */
    @GetMapping("/{id}/operation-logs")
    public ResponseBase<IPage<SkillOperationLogVO>> listOperationLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseBase.success(skillService.listOperationLogs(id, page, size));
    }

    /** 获取 Skill 版本历史 */
    @GetMapping("/{id}/versions")
    public ResponseBase<List<SkillVersionVO>> listVersions(@PathVariable Long id) {
        return ResponseBase.success(skillVersionLogic.listVersions(id));
    }

    /** 比较两个版本的差异 */
    @GetMapping("/{id}/versions/diff")
    public ResponseBase<Map<String, Object>> diffVersions(
            @PathVariable Long id,
            @RequestParam String v1,
            @RequestParam String v2) {
        SkillVersionEntity ver1 = skillVersionDAO.selectBySkillIdAndVersion(id, v1);
        SkillVersionEntity ver2 = skillVersionDAO.selectBySkillIdAndVersion(id, v2);
        if (ver1 == null || ver2 == null) {
            return ResponseBase.fail("版本不存在");
        }
        return ResponseBase.success(skillVersionLogic.diffVersions(ver1.getSnapshotPath(), ver2.getSnapshotPath()));
    }
}
