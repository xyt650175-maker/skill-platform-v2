package com.company.llmaif.skills.service.impl;

import com.company.llmaif.skills.logic.SkillDebugLogic;
import com.company.llmaif.skills.service.ISkillDebugService;
import com.company.llmaif.skills.service.vo.RunSkillDebugDTO;
import com.company.llmaif.skills.service.vo.SkillDebugVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SkillDebugServiceImpl implements ISkillDebugService {

    private final SkillDebugLogic skillDebugLogic;

    @Override
    public SkillDebugVO run(Long skillId, RunSkillDebugDTO dto) {
        return skillDebugLogic.run(skillId, dto);
    }
}
