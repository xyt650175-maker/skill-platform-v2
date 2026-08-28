package com.company.llmaif.git.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.git.dao.entity.GitCredentialEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GitCredentialDAO extends BaseMapper<GitCredentialEntity> {
}
