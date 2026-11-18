package com.qianxun.homestay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 认证模块可控业务错误，保留接口需要返回的 HTTP 状态。
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    /**
     * 创建带 HTTP 状态的业务异常。
     *
     * @param status HTTP 状态
     * @param message 面向调用方的消息
     */
    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
