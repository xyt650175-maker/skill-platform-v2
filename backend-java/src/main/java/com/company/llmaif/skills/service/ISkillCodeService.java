package com.company.llmaif.skills.service;

import com.company.llmaif.skills.service.vo.SaveSkillCodeDTO;
import com.company.llmaif.skills.service.vo.SkillCodeVO;

public interface ISkillCodeService {

    SkillCodeVO getCode(Long skillId);

    SkillCodeVO saveCode(Long skillId, SaveSkillCodeDTO dto);
}
