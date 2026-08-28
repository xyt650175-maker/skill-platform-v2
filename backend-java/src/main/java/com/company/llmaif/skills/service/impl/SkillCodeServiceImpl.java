package com.company.llmaif.skills.service.impl;

import com.company.llmaif.skills.logic.SkillCodeLogic;
import com.company.llmaif.skills.service.ISkillCodeService;
import com.company.llmaif.skills.service.vo.SaveSkillCodeDTO;
import com.company.llmaif.skills.service.vo.SkillCodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SkillCodeServiceImpl implements ISkillCodeService {

    private final SkillCodeLogic skillCodeLogic;

    @Override
    public SkillCodeVO getCode(Long skillId) {
        return skillCodeLogic.getCode(skillId);
    }

    @Override
    public SkillCodeVO saveCode(Long skillId, SaveSkillCodeDTO dto) {
        return skillCodeLogic.saveCode(skillId, dto);
    }
}
