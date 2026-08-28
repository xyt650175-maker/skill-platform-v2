package com.company.llmaif.skills.service;

import com.company.llmaif.skills.service.vo.CreateEvalTaskDTO;
import com.company.llmaif.skills.service.vo.EvalTaskVO;
import java.util.List;

public interface IEvalService {
    EvalTaskVO createEvalTask(CreateEvalTaskDTO dto, Long creatorId);
    EvalTaskVO getEvalTaskById(Long id);
    List<EvalTaskVO> listEvalTasks(String status, Long agentId);
    void updateStatus(Long id, String status);
}
