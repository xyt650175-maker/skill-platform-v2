package com.company.llmaif.auth.controller;

import com.company.llmaif.auth.service.IAuthService;
import com.company.llmaif.auth.service.impl.vo.LoginDTO;
import com.company.llmaif.auth.service.impl.vo.LoginVO;
import com.company.llmaif.common.ResponseBase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证接口
 * 注意：拦截器放行 /auth/login 与 /auth/register
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public ResponseBase<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseBase.success(authService.login(dto));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseBase<LoginVO.UserInfoVO> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseBase.success(authService.getCurrentUserInfo(userId));
    }
}
