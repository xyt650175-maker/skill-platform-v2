package com.company.llmaif.common;

/**
 * 未认证异常
 */
public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }

    public static UnauthorizedException of(String message) {
        return new UnauthorizedException(message);
    }
}
