package com.company.llmaif.skills.controller;

import com.company.llmaif.common.ResponseBase;
import com.company.llmaif.skills.service.IEvalService;
import com.company.llmaif.skills.service.vo.CreateEvalTaskDTO;
import com.company.llmaif.skills.service.vo.EvalTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eval-tasks")
@RequiredArgsConstructor
public class EvalController {

    private final IEvalService evalService;

    @PostMapping
    public ResponseBase<EvalTaskVO> create(@Validated @RequestBody CreateEvalTaskDTO dto,
                                            @RequestAttribute("userId") Long userId) {
        return ResponseBase.success(evalService.createEvalTask(dto, userId));
    }

    @GetMapping("/{id}")
    public ResponseBase<EvalTaskVO> getById(@PathVariable Long id) {
        return ResponseBase.success(evalService.getEvalTaskById(id));
    }

    @GetMapping
    public ResponseBase<List<EvalTaskVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long agentId) {
        return ResponseBase.success(evalService.listEvalTasks(status, agentId));
    }

    @PutMapping("/{id}/status")
    public ResponseBase<Void> updateStatus(@PathVariable Long id,
                                            @RequestParam String status) {
        evalService.updateStatus(id, status);
        return ResponseBase.success(null);
    }
}
