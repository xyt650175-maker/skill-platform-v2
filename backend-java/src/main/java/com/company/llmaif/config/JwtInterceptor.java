package com.company.llmaif.config;

import com.company.llmaif.common.UnauthorizedException;
import com.company.llmaif.team.logic.TeamContextLogic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final LlmaifProperties props;
    private final TeamContextLogic teamContextLogic;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(props.getJwt().getHeader());
        if (header == null || !header.startsWith(props.getJwt().getPrefix())) {
            throw UnauthorizedException.of("未登录或令牌缺失");
        }
        String token = header.substring(props.getJwt().getPrefix().length()).trim();
        if (!jwtUtils.validateToken(token)) {
            throw UnauthorizedException.of("令牌无效或已过期");
        }
        // 把用户信息放到 request 供 Controller 使用
        request.setAttribute("userId", jwtUtils.getUserId(token));
        request.setAttribute("username", jwtUtils.getUsername(token));
        request.setAttribute("role", jwtUtils.getRole(token));
        request.setAttribute("teamId", teamContextLogic.resolveTeamId(
                jwtUtils.getUserId(token), request.getHeader("X-Team-Id")));
        return true;
    }
}
