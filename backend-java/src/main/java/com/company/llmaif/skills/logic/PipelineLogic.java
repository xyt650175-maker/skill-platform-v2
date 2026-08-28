package com.company.llmaif.skills.logic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.llmaif.common.AgentException;
import com.company.llmaif.skills.dao.PipelineDAO;
import com.company.llmaif.skills.dao.entity.PipelineEntity;
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
public class PipelineLogic {

    private final PipelineDAO pipelineDAO;

    @Transactional(rollbackFor = Exception.class)
    public PipelineVO createPipeline(CreatePipelineDTO dto, Long creatorId) {
        PipelineEntity existing = pipelineDAO.selectByName(dto.getName());
        if (existing != null) {
            throw new AgentException("流水线名称已存在");
        }

        PipelineEntity entity = new PipelineEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setType(dto.getType());
        entity.setStages(dto.getStages());
        entity.setCurrentVersion("0.0.0");
        entity.setStatus("draft");
        entity.setCreatorId(creatorId);

        pipelineDAO.insert(entity);
        return convertToVO(entity);
    }

    public PipelineVO getPipelineById(Long id) {
        PipelineEntity entity = pipelineDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("流水线不存在");
        }
        return convertToVO(entity);
    }

    public List<PipelineVO> listPipelines(String type, String status) {
        LambdaQueryWrapper<PipelineEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(type)) {
            wrapper.eq(PipelineEntity::getType, type);
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(PipelineEntity::getStatus, status);
        }
        wrapper.orderByDesc(PipelineEntity::getCreateTime);

        return pipelineDAO.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public PipelineVO updatePipeline(Long id, CreatePipelineDTO dto) {
        PipelineEntity entity = pipelineDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("流水线不存在");
        }

        if (StringUtils.isNotBlank(dto.getDescription())) {
            entity.setDescription(dto.getDescription());
        }
        if (StringUtils.isNotBlank(dto.getStages())) {
            entity.setStages(dto.getStages());
        }
        if (StringUtils.isNotBlank(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }

        pipelineDAO.updateById(entity);
        return convertToVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePipeline(Long id) {
        PipelineEntity entity = pipelineDAO.selectById(id);
        if (entity == null) {
            throw new AgentException("流水线不存在");
        }
        pipelineDAO.deleteById(id);
    }

    private PipelineVO convertToVO(PipelineEntity entity) {
        PipelineVO vo = new PipelineVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setType(entity.getType());
        vo.setStages(entity.getStages());
        vo.setCurrentVersion(entity.getCurrentVersion());
        vo.setStatus(entity.getStatus());
        vo.setCreatorId(entity.getCreatorId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
