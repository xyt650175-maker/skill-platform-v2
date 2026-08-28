package com.company.llmaif.skills.service;

import com.company.llmaif.skills.service.vo.CreatePipelineDTO;
import com.company.llmaif.skills.service.vo.PipelineVO;
import java.util.List;

public interface IPipelineService {
    PipelineVO createPipeline(CreatePipelineDTO dto, Long creatorId);
    PipelineVO getPipelineById(Long id);
    List<PipelineVO> listPipelines(String type, String status);
    PipelineVO updatePipeline(Long id, CreatePipelineDTO dto);
    void deletePipeline(Long id);
}
