package com.company.llmaif.git.controller;

import com.company.llmaif.common.ResponseBase;
import com.company.llmaif.git.logic.EnterpriseGitLogic;
import com.company.llmaif.git.service.vo.GitCredentialDTO;
import com.company.llmaif.git.service.vo.GitCredentialVO;
import com.company.llmaif.git.service.vo.EnterpriseGitSkillVO;
import com.company.llmaif.git.service.vo.EnterpriseGitRefsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enterprise-git/credentials")
@RequiredArgsConstructor
public class EnterpriseGitController {
    private final EnterpriseGitLogic enterpriseGitLogic;
    @GetMapping public ResponseBase<List<GitCredentialVO>> list(@RequestAttribute("userId") Long userId, @RequestAttribute("teamId") Long teamId) { return ResponseBase.success(enterpriseGitLogic.list(userId, teamId)); }
    @PostMapping public ResponseBase<GitCredentialVO> save(@Validated @RequestBody GitCredentialDTO dto, @RequestAttribute("userId") Long userId, @RequestAttribute("teamId") Long teamId) { return ResponseBase.success(enterpriseGitLogic.save(dto, userId, teamId)); }
    @DeleteMapping("/{id}") public ResponseBase<Void> delete(@PathVariable Long id, @RequestAttribute("userId") Long userId, @RequestAttribute("teamId") Long teamId) { enterpriseGitLogic.delete(id, userId, teamId); return ResponseBase.success(null); }
    @PostMapping("/{id}/test") public ResponseBase<GitCredentialVO> test(@PathVariable Long id, @RequestAttribute("userId") Long userId, @RequestAttribute("teamId") Long teamId) { return ResponseBase.success(enterpriseGitLogic.test(id, userId, teamId)); }

    @GetMapping("/{id}/refs")
    public ResponseBase<EnterpriseGitRefsVO> refs(@PathVariable Long id, @RequestAttribute("userId") Long userId, @RequestAttribute("teamId") Long teamId) {
        return ResponseBase.success(enterpriseGitLogic.listRefs(id, userId, teamId));
    }

    /** 由后端使用已托管的企业令牌读取指定分支或 Tag；令牌不会返回浏览器。 */
    @GetMapping("/{id}/skills")
    public ResponseBase<List<EnterpriseGitSkillVO>> loadSkills(@PathVariable Long id,
                                                                 @RequestParam String ref,
                                                                 @RequestAttribute("userId") Long userId, @RequestAttribute("teamId") Long teamId) {
        return ResponseBase.success(enterpriseGitLogic.loadSkills(id, ref, userId, teamId));
    }
}
