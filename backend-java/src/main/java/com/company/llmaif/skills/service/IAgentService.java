package com.company.llmaif.skills.service;

import com.company.llmaif.skills.service.vo.*;

import java.util.List;

public interface IAgentService {

    AgentVO createAgent(CreateAgentDTO dto, Long creatorId);

    AgentVO updateAgent(UpdateAgentDTO dto);

    void deleteAgent(Long id);

    AgentVO getAgentById(Long id);

    List<AgentVO> listAgents(String status, Long creatorId);

    void submitCanvas(Long agentId, String canvasConfig);

    // SubAgent
    SubAgentVO createSubAgent(Long agentId, CreateSubAgentDTO dto);

    List<SubAgentVO> listSubAgents(Long agentId);

    // Skill Mounting
    SkillMountingVO createMounting(Long agentId, CreateSkillMountingDTO dto);

    SkillMountingVO updateMounting(Long agentId, Long mountingId, UpdateSkillMountingDTO dto);

    List<SkillMountingVO> listMountings(Long agentId);

    List<SkillMountingVO> listMountingsByVersion(Long agentId, String version);

    // Agent Version
    AgentVersionVO createAgentVersion(Long agentId, CreateAgentVersionDTO dto, Long creatorId);

    List<AgentVersionVO> listAgentVersions(Long agentId);

    AgentVO switchAgentVersion(Long agentId, String version);
}
