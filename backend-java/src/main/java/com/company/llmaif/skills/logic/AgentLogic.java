package com.company.llmaif.skills.logic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.llmaif.common.AgentException;
import com.company.llmaif.skills.dao.*;
import com.company.llmaif.skills.dao.entity.*;
import com.company.llmaif.skills.service.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLogic {

    private final AgentDAO agentDAO;
    private final SubAgentDAO subAgentDAO;
    private final SkillMountingDAO skillMountingDAO;
    private final SkillDAO skillDAO;
    private final AgentVersionDAO agentVersionDAO;

    @Transactional(rollbackFor = Exception.class)
    public AgentVO createAgent(CreateAgentDTO dto, Long creatorId) {
        AgentEntity existing = agentDAO.selectByName(dto.getName());
        if (existing != null) {
            throw new AgentException("Agent 名称已存在");
        }

        AgentEntity entity = new AgentEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setModelName(dto.getModelName());
        entity.setSystemPrompt(dto.getSystemPrompt());
        entity.setCanvasConfig(dto.getCanvasConfig());
        entity.setCurrentVersion("0.0.0");
        entity.setStatus("draft");
        entity.setCreatorId(creatorId);

        agentDAO.insert(entity);
        return convertToVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentVO updateAgent(UpdateAgentDTO dto) {
        AgentEntity entity = agentDAO.selectById(dto.getId());
        if (entity == null) {
            throw new AgentException("Agent 不存在");
        }

        if (StringUtils.isNotBlank(dto.getDescription())) {
            entity.setDescription(dto.getDescription());
        }
        if (StringUtils.isNotBlank(dto.getModelName())) {
            entity.setModelName(dto.getModelName());
        }
        if (StringUtils.isNotBlank(dto.getSystemPrompt())) {
            entity.setSystemPrompt(dto.getSystemPrompt());
        }
        if (StringUtils.isNotBlank(dto.getCanvasConfig())) {
            entity.setCanvasConfig(dto.getCanvasConfig());
        }
        if (StringUtils.isNotBlank(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }

        agentDAO.updateById(entity);
        return convertToVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(Long id) {
        AgentEntity entity = agentDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("Agent 不存在");
        }
        agentDAO.deleteById(id);
    }

    public AgentVO getAgentById(Long id) {
        AgentEntity entity = agentDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("Agent 不存在");
        }
        return convertToVO(entity);
    }

    public List<AgentVO> listAgents(String status, Long creatorId) {
        LambdaQueryWrapper<AgentEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(AgentEntity::getStatus, status);
        }
        if (creatorId != null) {
            wrapper.eq(AgentEntity::getCreatorId, creatorId);
        }
        wrapper.orderByDesc(AgentEntity::getCreateTime);
        return agentDAO.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitCanvas(Long agentId, String canvasConfig) {
        AgentEntity entity = agentDAO.selectById(agentId);
        if (entity == null) {
            throw new AgentException("Agent 不存在");
        }
        entity.setCanvasConfig(canvasConfig);
        agentDAO.updateById(entity);
    }

    // ========== SubAgent ==========

    @Transactional(rollbackFor = Exception.class)
    public SubAgentVO createSubAgent(Long agentId, CreateSubAgentDTO dto) {
        AgentEntity agent = agentDAO.selectById(agentId);
        if (agent == null) {
            throw new AgentException("Agent 不存在");
        }

        SubAgentEntity entity = new SubAgentEntity();
        entity.setAgentId(agentId);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setSystemPrompt(dto.getSystemPrompt());
        entity.setModelName(dto.getModelName());

        subAgentDAO.insert(entity);
        return convertToSubAgentVO(entity);
    }

    public List<SubAgentVO> listSubAgents(Long agentId) {
        return subAgentDAO.selectByAgentId(agentId).stream()
                .map(this::convertToSubAgentVO)
                .collect(Collectors.toList());
    }

    // ========== Skill Mounting ==========

    @Transactional(rollbackFor = Exception.class)
    public SkillMountingVO createMounting(Long agentId, CreateSkillMountingDTO dto) {
        AgentEntity agent = agentDAO.selectById(agentId);
        if (agent == null) {
            throw new AgentException("Agent 不存在");
        }

        SkillEntity skill = skillDAO.selectById(dto.getSkillId());
        if (skill == null) {
            throw new AgentException("Skill 不存在");
        }

        SkillMountingEntity entity = new SkillMountingEntity();
        entity.setAgentId(agentId);
        entity.setSubAgentId(dto.getSubAgentId());
        entity.setSkillId(dto.getSkillId());
        entity.setSkillAlias(dto.getSkillAlias());
        entity.setAgentVersion(dto.getAgentVersion());
        entity.setEnabled(dto.getEnabled() ? 1 : 0);

        skillMountingDAO.insert(entity);
        return convertToMountingVO(entity, skill);
    }

    @Transactional(rollbackFor = Exception.class)
    public SkillMountingVO updateMounting(Long agentId, Long mountingId, UpdateSkillMountingDTO dto) {
        SkillMountingEntity entity = skillMountingDAO.selectById(mountingId);
        if (entity == null) {
            throw new AgentException("Skill 挂载记录不存在");
        }

        if (dto.getAgentVersion() != null) {
            entity.setAgentVersion(dto.getAgentVersion());
        }
        if (dto.getSkillAlias() != null) {
            entity.setSkillAlias(dto.getSkillAlias());
        }
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled() ? 1 : 0);
        }

        skillMountingDAO.updateById(entity);
        SkillEntity skill = skillDAO.selectById(entity.getSkillId());
        return convertToMountingVO(entity, skill);
    }

    public List<SkillMountingVO> listMountings(Long agentId) {
        List<SkillMountingEntity> mountings = skillMountingDAO.selectByAgentId(agentId);
        return mountings.stream()
                .map(m -> {
                    SkillEntity skill = skillDAO.selectById(m.getSkillId());
                    return convertToMountingVO(m, skill);
                })
                .collect(Collectors.toList());
    }

    public List<SkillMountingVO> listMountingsByVersion(Long agentId, String version) {
        List<SkillMountingEntity> mountings = skillMountingDAO.selectByAgentIdAndVersion(agentId, version);
        return mountings.stream()
                .map(m -> {
                    SkillEntity skill = skillDAO.selectById(m.getSkillId());
                    return convertToMountingVO(m, skill);
                })
                .collect(Collectors.toList());
    }

    // ========== 转换方法 ==========

    private AgentVO convertToVO(AgentEntity entity) {
        AgentVO vo = new AgentVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setCurrentVersion(entity.getCurrentVersion());
        vo.setStatus(entity.getStatus());
        vo.setModelName(entity.getModelName());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setCanvasConfig(entity.getCanvasConfig());
        vo.setCreatorId(entity.getCreatorId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setVersions(listAgentVersions(entity.getId()));
        return vo;
    }

    private SubAgentVO convertToSubAgentVO(SubAgentEntity entity) {
        SubAgentVO vo = new SubAgentVO();
        vo.setId(entity.getId());
        vo.setAgentId(entity.getAgentId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setModelName(entity.getModelName());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private SkillMountingVO convertToMountingVO(SkillMountingEntity entity, SkillEntity skill) {
        SkillMountingVO vo = new SkillMountingVO();
        vo.setId(entity.getId());
        vo.setAgentId(entity.getAgentId());
        vo.setSubAgentId(entity.getSubAgentId());
        vo.setSkillId(entity.getSkillId());
        vo.setSkillName(skill != null ? skill.getName() : null);
        vo.setSkillAlias(entity.getSkillAlias());
        vo.setAgentVersion(entity.getAgentVersion());
        vo.setEnabled(entity.getEnabled() == 1);
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    // ========== Agent Version ==========

    @Transactional(rollbackFor = Exception.class)
    public AgentVersionVO createAgentVersion(Long agentId, CreateAgentVersionDTO dto, Long creatorId) {
        AgentEntity agent = agentDAO.selectById(agentId);
        if (agent == null) {
            throw new AgentException("Agent 不存在");
        }

        AgentVersionEntity existing = agentVersionDAO.selectByAgentIdAndVersion(agentId, dto.getVersion());
        if (existing != null) {
            throw new AgentException("该版本已存在");
        }

        AgentVersionEntity entity = new AgentVersionEntity();
        entity.setAgentId(agentId);
        entity.setVersion(dto.getVersion());
        entity.setChangeSummary(dto.getChangeSummary());
        entity.setCreatorId(creatorId);

        agentVersionDAO.insert(entity);

        // 更新 agent 的当前版本
        agent.setCurrentVersion(dto.getVersion());
        agentDAO.updateById(agent);

        return convertToVersionVO(entity);
    }

    public List<AgentVersionVO> listAgentVersions(Long agentId) {
        return agentVersionDAO.selectByAgentId(agentId).stream()
                .map(this::convertToVersionVO)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentVO switchAgentVersion(Long agentId, String version) {
        AgentEntity agent = agentDAO.selectById(agentId);
        if (agent == null) {
            throw new AgentException("Agent 不存在");
        }

        AgentVersionEntity versionEntity = agentVersionDAO.selectByAgentIdAndVersion(agentId, version);
        if (versionEntity == null) {
            throw new AgentException("版本不存在");
        }

        agent.setCurrentVersion(version);
        agentDAO.updateById(agent);

        return convertToVO(agent);
    }

    private AgentVersionVO convertToVersionVO(AgentVersionEntity entity) {
        AgentVersionVO vo = new AgentVersionVO();
        vo.setId(entity.getId());
        vo.setAgentId(entity.getAgentId());
        vo.setVersion(entity.getVersion());
        vo.setChangeSummary(entity.getChangeSummary());
        vo.setCreatorId(entity.getCreatorId());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
