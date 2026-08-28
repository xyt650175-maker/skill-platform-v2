package com.company.llmaif.auth.logic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.llmaif.auth.dao.UserDAO;
import com.company.llmaif.auth.dao.entity.UserEntity;
import com.company.llmaif.common.AgentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 用户业务逻辑层
 * 复杂业务规则、跨表聚合
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLogic {

    private final UserDAO userDAO;

    /**
     * 根据用户名查询用户
     */
    public UserEntity findByUsername(String username) {
        return userDAO.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, username)
        );
    }

    /**
     * 根据ID查询
     */
    public UserEntity findById(Long id) {
        return userDAO.selectById(id);
    }

    /**
     * 校验用户状态
     */
    public void checkUserActive(UserEntity user) {
        if (user == null) {
            throw new AgentException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new AgentException("用户已被禁用");
        }
    }

    /**
     * 根据角色获取权限列表
     */
    public List<String> getPermissionsByRole(String role) {
        switch (role == null ? "" : role) {
            case "admin":
                return Arrays.asList("skill:read", "skill:write", "skill:delete",
                        "agent:read", "agent:write", "agent:delete",
                        "test:read", "test:write", "publish", "approve", "manage");
            case "developer":
                return Arrays.asList("skill:read", "skill:write", "agent:read", "agent:write", "test:read", "test:write");
            case "qa":
                return Arrays.asList("skill:read", "agent:read", "test:read", "test:write", "approve");
            case "viewer":
                return Arrays.asList("skill:read", "agent:read", "test:read");
            default:
                return Arrays.asList("skill:read");
        }
    }
}
