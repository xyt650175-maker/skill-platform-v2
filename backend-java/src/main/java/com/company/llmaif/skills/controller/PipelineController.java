package com.company.llmaif.skills.controller;

import com.company.llmaif.common.ResponseBase;
import com.company.llmaif.skills.service.IPipelineService;
import com.company.llmaif.skills.service.vo.CreatePipelineDTO;
import com.company.llmaif.skills.service.vo.PipelineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pipelines")
@RequiredArgsConstructor
public class PipelineController {

    private final IPipelineService pipelineService;

    @PostMapping
    public ResponseBase<PipelineVO> create(@Validated @RequestBody CreatePipelineDTO dto,
                                            @RequestAttribute("userId") Long userId) {
        return ResponseBase.success(pipelineService.createPipeline(dto, userId));
    }

    @GetMapping("/{id}")
    public ResponseBase<PipelineVO> getById(@PathVariable Long id) {
        return ResponseBase.success(pipelineService.getPipelineById(id));
    }

    @GetMapping
    public ResponseBase<List<PipelineVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ResponseBase.success(pipelineService.listPipelines(type, status));
    }

    @PutMapping("/{id}")
    public ResponseBase<PipelineVO> update(@PathVariable Long id,
                                            @Validated @RequestBody CreatePipelineDTO dto) {
        return ResponseBase.success(pipelineService.updatePipeline(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseBase<Void> delete(@PathVariable Long id) {
        pipelineService.deletePipeline(id);
        return ResponseBase.success(null);
    }
}
