package com.company.llmaif.auth.service;

import com.company.llmaif.auth.service.impl.vo.LoginDTO;
import com.company.llmaif.auth.service.impl.vo.LoginVO;

/**
 * 认证服务接口
 */
public interface IAuthService {

    /**
     * 登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前用户信息
     */
    LoginVO.UserInfoVO getCurrentUserInfo(Long userId);
}
