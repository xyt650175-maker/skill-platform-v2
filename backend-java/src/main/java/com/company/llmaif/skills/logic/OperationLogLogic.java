package com.company.llmaif.skills.logic;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.llmaif.skills.dao.SkillOperationLogDAO;
import com.company.llmaif.skills.dao.entity.SkillOperationLogEntity;
import com.company.llmaif.skills.service.vo.SkillOperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志逻辑
 */
@Component
@RequiredArgsConstructor
public class OperationLogLogic {

    private final SkillOperationLogDAO operationLogDAO;

    private static final Map<String, String> ACTION_LABELS = new HashMap<>();
    static {
        ACTION_LABELS.put("save_draft", "保存草稿");
        ACTION_LABELS.put("submit_review", "提交评审");
        ACTION_LABELS.put("approve", "审核通过");
        ACTION_LABELS.put("reject", "审核驳回");
        ACTION_LABELS.put("publish", "定版发布");
        ACTION_LABELS.put("version_switch", "版本切换");
    }

    /**
     * 记录操作日志
     */
    public void log(Long skillId, Long operatorId, String operatorName,
                    String action, String description, String changeSummary,
                    String status, String version) {
        SkillOperationLogEntity entity = new SkillOperationLogEntity();
        entity.setSkillId(skillId);
        entity.setOperatorId(operatorId);
        entity.setOperatorName(operatorName);
        entity.setAction(action);
        entity.setDescription(description);
        entity.setChangeSummary(changeSummary);
        entity.setStatus(status);
        entity.setVersion(version);
        operationLogDAO.insert(entity);
    }

    /**
     * 分页查询操作日志
     */
    public IPage<SkillOperationLogVO> listLogs(Long skillId, int page, int size) {
        Page<SkillOperationLogEntity> pageParam = new Page<>(page, size);
        IPage<SkillOperationLogEntity> entityPage = operationLogDAO.selectPageBySkillId(pageParam, skillId);

        Page<SkillOperationLogVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::convertToVO).collect(java.util.stream.Collectors.toList()));
        return voPage;
    }

    private SkillOperationLogVO convertToVO(SkillOperationLogEntity entity) {
        SkillOperationLogVO vo = new SkillOperationLogVO();
        vo.setId(entity.getId());
        vo.setSkillId(entity.getSkillId());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setAction(entity.getAction());
        vo.setActionLabel(ACTION_LABELS.getOrDefault(entity.getAction(), entity.getAction()));
        vo.setDescription(entity.getDescription());
        vo.setChangeSummary(entity.getChangeSummary());
        vo.setStatus(entity.getStatus());
        vo.setVersion(entity.getVersion());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
