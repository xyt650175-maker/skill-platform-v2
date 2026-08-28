package com.company.llmaif.skills.logic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.llmaif.common.AgentException;
import com.company.llmaif.skills.dao.AgentDAO;
import com.company.llmaif.skills.dao.EvalTaskDAO;
import com.company.llmaif.skills.dao.entity.AgentEntity;
import com.company.llmaif.skills.dao.entity.EvalTaskEntity;
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
public class EvalLogic {

    private final EvalTaskDAO evalTaskDAO;
    private final AgentDAO agentDAO;

    @Transactional(rollbackFor = Exception.class)
    public EvalTaskVO createEvalTask(CreateEvalTaskDTO dto, Long creatorId) {
        AgentEntity agent = agentDAO.selectById(dto.getAgentId());
        if (agent == null) {
            throw new AgentException("Agent 不存在");
        }

        EvalTaskEntity entity = new EvalTaskEntity();
        entity.setName(dto.getName());
        entity.setAgentId(dto.getAgentId());
        entity.setAgentVersion(dto.getAgentVersion());
        entity.setDatasetKey(dto.getDatasetKey());
        entity.setDatasetVersion(dto.getDatasetVersion());
        entity.setScope(dto.getScope());
        entity.setStatus("pending");
        entity.setCreatorId(creatorId);

        evalTaskDAO.insert(entity);
        return convertToVO(entity, agent);
    }

    public EvalTaskVO getEvalTaskById(Long id) {
        EvalTaskEntity entity = evalTaskDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("评测任务不存在");
        }
        AgentEntity agent = agentDAO.selectById(entity.getAgentId());
        return convertToVO(entity, agent);
    }

    public List<EvalTaskVO> listEvalTasks(String status, Long agentId) {
        LambdaQueryWrapper<EvalTaskEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(EvalTaskEntity::getStatus, status);
        }
        if (agentId != null) {
            wrapper.eq(EvalTaskEntity::getAgentId, agentId);
        }
        wrapper.orderByDesc(EvalTaskEntity::getCreateTime);

        List<EvalTaskEntity> entities = evalTaskDAO.selectList(wrapper);
        return entities.stream()
                .map(e -> {
                    AgentEntity agent = agentDAO.selectById(e.getAgentId());
                    return convertToVO(e, agent);
                })
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        EvalTaskEntity entity = evalTaskDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("评测任务不存在");
        }
        entity.setStatus(status);
        evalTaskDAO.updateById(entity);
    }

    private EvalTaskVO convertToVO(EvalTaskEntity entity, AgentEntity agent) {
        EvalTaskVO vo = new EvalTaskVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setAgentId(entity.getAgentId());
        vo.setAgentName(agent != null ? agent.getName() : null);
        vo.setAgentVersion(entity.getAgentVersion());
        vo.setDatasetKey(entity.getDatasetKey());
        vo.setDatasetVersion(entity.getDatasetVersion());
        vo.setScope(entity.getScope());
        vo.setStatus(entity.getStatus());
        vo.setResultSummary(entity.getResultSummary());
        vo.setCreatorId(entity.getCreatorId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
