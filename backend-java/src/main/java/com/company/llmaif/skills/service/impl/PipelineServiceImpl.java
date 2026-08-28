package com.company.llmaif.skills.service.impl;

import com.company.llmaif.skills.logic.PipelineLogic;
import com.company.llmaif.skills.service.IPipelineService;
import com.company.llmaif.skills.service.vo.CreatePipelineDTO;
import com.company.llmaif.skills.service.vo.PipelineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements IPipelineService {

    private final PipelineLogic pipelineLogic;

    @Override
    public PipelineVO createPipeline(CreatePipelineDTO dto, Long creatorId) {
        return pipelineLogic.createPipeline(dto, creatorId);
    }

    @Override
    public PipelineVO getPipelineById(Long id) {
        return pipelineLogic.getPipelineById(id);
    }

    @Override
    public List<PipelineVO> listPipelines(String type, String status) {
        return pipelineLogic.listPipelines(type, status);
    }

    @Override
    public PipelineVO updatePipeline(Long id, CreatePipelineDTO dto) {
        return pipelineLogic.updatePipeline(id, dto);
    }

    @Override
    public void deletePipeline(Long id) {
        pipelineLogic.deletePipeline(id);
    }
}
