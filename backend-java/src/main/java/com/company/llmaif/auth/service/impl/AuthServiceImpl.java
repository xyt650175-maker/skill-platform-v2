package com.company.llmaif.auth.service.impl;

import com.company.llmaif.auth.dao.entity.UserEntity;
import com.company.llmaif.auth.logic.UserLogic;
import com.company.llmaif.auth.service.IAuthService;
import com.company.llmaif.auth.service.impl.vo.LoginDTO;
import com.company.llmaif.auth.service.impl.vo.LoginVO;
import com.company.llmaif.common.AgentException;
import com.company.llmaif.config.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UserLogic userLogic;
    private final JwtUtils jwtUtils;

    @Override
    public LoginVO login(LoginDTO dto) {
        UserEntity user = userLogic.findByUsername(dto.getUsername());
        userLogic.checkUserActive(user);

        // 密码校验（示例使用明文，生产环境必须用 BCrypt）
        if (!dto.getPassword().equals(user.getPassword())) {
            throw new AgentException("用户名或密码错误");
        }

        // 生成 JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 组装返回
        LoginVO vo = new LoginVO();
        vo.setAccessToken(token);
        LoginVO.UserInfoVO info = new LoginVO.UserInfoVO();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setDisplayName(user.getDisplayName());
        info.setEmail(user.getEmail());
        info.setRole(user.getRole());
        List<String> perms = userLogic.getPermissionsByRole(user.getRole());
        info.setPermissions(perms);
        vo.setUser(info);

        log.info("用户登录成功: {}", user.getUsername());
        return vo;
    }

    @Override
    public LoginVO.UserInfoVO getCurrentUserInfo(Long userId) {
        UserEntity user = userLogic.findById(userId);
        if (user == null) {
            throw new AgentException("用户不存在");
        }
        LoginVO.UserInfoVO info = new LoginVO.UserInfoVO();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setDisplayName(user.getDisplayName());
        info.setEmail(user.getEmail());
        info.setRole(user.getRole());
        info.setPermissions(userLogic.getPermissionsByRole(user.getRole()));
        return info;
    }
}
