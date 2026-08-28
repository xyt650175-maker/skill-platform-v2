package com.company.llmaif.auth.service.impl.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录返回 VO
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String tokenType = "Bearer";
    private UserInfoVO user;

    @Data
    public static class UserInfoVO implements Serializable {
        private Long id;
        private String username;
        private String displayName;
        private String email;
        private String role;
        private List<String> permissions;
    }
}
