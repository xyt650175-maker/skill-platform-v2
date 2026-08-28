package com.company.llmaif.skills.service;

import com.company.llmaif.skills.service.vo.RunSkillDebugDTO;
import com.company.llmaif.skills.service.vo.SkillDebugVO;

public interface ISkillDebugService {

    SkillDebugVO run(Long skillId, RunSkillDebugDTO dto);
}
