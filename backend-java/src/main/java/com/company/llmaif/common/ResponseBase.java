package com.company.llmaif.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应格式
 * code="0" 为成功，其他为失败
 */
@Data
public class ResponseBase<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CODE_SUCCESS = "0";
    public static final String CODE_FAIL = "1";

    private String code;
    private String message;
    private T data;

    public ResponseBase() {
    }

    public ResponseBase(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseBase<T> success(T data) {
        return new ResponseBase<>(CODE_SUCCESS, "success", data);
    }

    public static <T> ResponseBase<T> success() {
        return new ResponseBase<>(CODE_SUCCESS, "success", null);
    }

    public static <T> ResponseBase<T> fail(String message) {
        return new ResponseBase<>(CODE_FAIL, message, null);
    }

    public static <T> ResponseBase<T> fail(String code, String message) {
        return new ResponseBase<>(code, message, null);
    }

    public boolean isSuccess() {
        return CODE_SUCCESS.equals(this.code);
    }
}
