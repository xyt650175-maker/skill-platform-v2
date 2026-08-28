package com.company.llmaif.common;

import lombok.Getter;

/**
 * 业务异常基类
 * 全局由 GlobalExceptionHandler 捕获
 */
@Getter
public class AgentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public AgentException(String message) {
        super(message);
        this.code = ResponseBase.CODE_FAIL;
    }

    public AgentException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AgentException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
