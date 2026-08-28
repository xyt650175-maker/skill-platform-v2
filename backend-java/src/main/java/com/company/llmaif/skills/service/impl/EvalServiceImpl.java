package com.company.llmaif.skills.service.impl;

import com.company.llmaif.skills.logic.EvalLogic;
import com.company.llmaif.skills.service.IEvalService;
import com.company.llmaif.skills.service.vo.CreateEvalTaskDTO;
import com.company.llmaif.skills.service.vo.EvalTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvalServiceImpl implements IEvalService {

    private final EvalLogic evalLogic;

    @Override
    public EvalTaskVO createEvalTask(CreateEvalTaskDTO dto, Long creatorId) {
        return evalLogic.createEvalTask(dto, creatorId);
    }

    @Override
    public EvalTaskVO getEvalTaskById(Long id) {
        return evalLogic.getEvalTaskById(id);
    }

    @Override
    public List<EvalTaskVO> listEvalTasks(String status, Long agentId) {
        return evalLogic.listEvalTasks(status, agentId);
    }

    @Override
    public void updateStatus(Long id, String status) {
        evalLogic.updateStatus(id, status);
    }
}
