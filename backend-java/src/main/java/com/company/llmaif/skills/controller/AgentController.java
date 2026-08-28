package com.company.llmaif.skills.controller;

import com.company.llmaif.common.ResponseBase;
import com.company.llmaif.skills.service.IAgentService;
import com.company.llmaif.skills.service.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final IAgentService agentService;

    @PostMapping
    public ResponseBase<AgentVO> create(@Validated @RequestBody CreateAgentDTO dto,
                                         @RequestAttribute("userId") Long userId) {
        return ResponseBase.success(agentService.createAgent(dto, userId));
    }

    @PutMapping
    public ResponseBase<AgentVO> update(@Validated @RequestBody UpdateAgentDTO dto) {
        return ResponseBase.success(agentService.updateAgent(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseBase<Void> delete(@PathVariable Long id) {
        agentService.deleteAgent(id);
        return ResponseBase.success(null);
    }

    @GetMapping("/{id}")
    public ResponseBase<AgentVO> getById(@PathVariable Long id) {
        return ResponseBase.success(agentService.getAgentById(id));
    }

    @GetMapping
    public ResponseBase<List<AgentVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long creatorId) {
        return ResponseBase.success(agentService.listAgents(status, creatorId));
    }

    @PutMapping("/{id}/canvas")
    public ResponseBase<Void> submitCanvas(@PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        agentService.submitCanvas(id, body.get("canvas"));
        return ResponseBase.success(null);
    }

    // SubAgent
    @GetMapping("/{agentId}/sub-agents")
    public ResponseBase<List<SubAgentVO>> listSubAgents(@PathVariable Long agentId) {
        return ResponseBase.success(agentService.listSubAgents(agentId));
    }

    @PostMapping("/{agentId}/sub-agents")
    public ResponseBase<SubAgentVO> createSubAgent(@PathVariable Long agentId,
                                                    @Validated @RequestBody CreateSubAgentDTO dto) {
        return ResponseBase.success(agentService.createSubAgent(agentId, dto));
    }

    // Skill Mounting
    @GetMapping("/{agentId}/mountings")
    public ResponseBase<List<SkillMountingVO>> listMountings(@PathVariable Long agentId,
                                                              @RequestParam(required = false) String version) {
        if (version != null) {
            return ResponseBase.success(agentService.listMountingsByVersion(agentId, version));
        }
        return ResponseBase.success(agentService.listMountings(agentId));
    }

    @PostMapping("/{agentId}/mountings")
    public ResponseBase<SkillMountingVO> createMounting(@PathVariable Long agentId,
                                                         @Validated @RequestBody CreateSkillMountingDTO dto) {
        return ResponseBase.success(agentService.createMounting(agentId, dto));
    }

    @PutMapping("/{agentId}/mountings/{mountingId}")
    public ResponseBase<SkillMountingVO> updateMounting(@PathVariable Long agentId,
                                                         @PathVariable Long mountingId,
                                                         @RequestBody UpdateSkillMountingDTO dto) {
        return ResponseBase.success(agentService.updateMounting(agentId, mountingId, dto));
    }

    // Agent Version
    @GetMapping("/{agentId}/versions")
    public ResponseBase<List<AgentVersionVO>> listVersions(@PathVariable Long agentId) {
        return ResponseBase.success(agentService.listAgentVersions(agentId));
    }

    @PostMapping("/{agentId}/versions")
    public ResponseBase<AgentVersionVO> createVersion(@PathVariable Long agentId,
                                                       @Validated @RequestBody CreateAgentVersionDTO dto,
                                                       @RequestAttribute("userId") Long userId) {
        return ResponseBase.success(agentService.createAgentVersion(agentId, dto, userId));
    }

    @PutMapping("/{agentId}/versions/{version}")
    public ResponseBase<AgentVO> switchVersion(@PathVariable Long agentId,
                                                @PathVariable String version) {
        return ResponseBase.success(agentService.switchAgentVersion(agentId, version));
    }

    // DAG 配置获取（HTTP 解耦）
    @GetMapping("/{agentId}/config")
    public ResponseBase<Map<String, Object>> getConfig(@PathVariable Long agentId,
                                                        @RequestParam(required = false) String version) {
        AgentVO agent = agentService.getAgentById(agentId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("agent_config", agent.getCanvasConfig());
        result.put("source", "local");
        return ResponseBase.success(result);
    }
}
