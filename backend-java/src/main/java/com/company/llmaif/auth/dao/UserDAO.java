package com.company.llmaif.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.llmaif.auth.dao.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 DAO
 */
@Mapper
public interface UserDAO extends BaseMapper<UserEntity> {
}
