package com.company.llmaif.skills.service.impl;

import com.company.llmaif.skills.logic.AgentLogic;
import com.company.llmaif.skills.service.IAgentService;
import com.company.llmaif.skills.service.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final AgentLogic agentLogic;

    @Override
    public AgentVO createAgent(CreateAgentDTO dto, Long creatorId) {
        return agentLogic.createAgent(dto, creatorId);
    }

    @Override
    public AgentVO updateAgent(UpdateAgentDTO dto) {
        return agentLogic.updateAgent(dto);
    }

    @Override
    public void deleteAgent(Long id) {
        agentLogic.deleteAgent(id);
    }

    @Override
    public AgentVO getAgentById(Long id) {
        return agentLogic.getAgentById(id);
    }

    @Override
    public List<AgentVO> listAgents(String status, Long creatorId) {
        return agentLogic.listAgents(status, creatorId);
    }

    @Override
    public void submitCanvas(Long agentId, String canvasConfig) {
        agentLogic.submitCanvas(agentId, canvasConfig);
    }

    @Override
    public SubAgentVO createSubAgent(Long agentId, CreateSubAgentDTO dto) {
        return agentLogic.createSubAgent(agentId, dto);
    }

    @Override
    public List<SubAgentVO> listSubAgents(Long agentId) {
        return agentLogic.listSubAgents(agentId);
    }

    @Override
    public SkillMountingVO createMounting(Long agentId, CreateSkillMountingDTO dto) {
        return agentLogic.createMounting(agentId, dto);
    }

    @Override
    public SkillMountingVO updateMounting(Long agentId, Long mountingId, UpdateSkillMountingDTO dto) {
        return agentLogic.updateMounting(agentId, mountingId, dto);
    }

    @Override
    public List<SkillMountingVO> listMountings(Long agentId) {
        return agentLogic.listMountings(agentId);
    }

    @Override
    public List<SkillMountingVO> listMountingsByVersion(Long agentId, String version) {
        return agentLogic.listMountingsByVersion(agentId, version);
    }

    @Override
    public AgentVersionVO createAgentVersion(Long agentId, CreateAgentVersionDTO dto, Long creatorId) {
        return agentLogic.createAgentVersion(agentId, dto, creatorId);
    }

    @Override
    public List<AgentVersionVO> listAgentVersions(Long agentId) {
        return agentLogic.listAgentVersions(agentId);
    }

    @Override
    public AgentVO switchAgentVersion(Long agentId, String version) {
        return agentLogic.switchAgentVersion(agentId, version);
    }
}
