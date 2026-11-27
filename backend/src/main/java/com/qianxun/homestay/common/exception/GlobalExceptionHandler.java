package com.qianxun.homestay.common.exception;

import com.qianxun.homestay.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 将请求参数、业务错误及 HTTP 方法错误转换为统一响应结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 将路由不支持的 HTTP 方法映射为 405。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getStatus().value(), exception.getMessage()));
    }

    /** 将参数校验失败和缺少请求字段统一映射为 400。 */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        String message = "请求参数不正确";
        if (exception instanceof MethodArgumentNotValidException validationException
                && validationException.getBindingResult().getFieldError() != null) {
            message = validationException.getBindingResult().getFieldError().getDefaultMessage();
        } else if (exception instanceof ConstraintViolationException validationException) {
            message = validationException.getConstraintViolations().stream()
                    .findFirst()
                    .map(violation -> violation.getMessage())
                    .orElse(message);
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务暂时不可用"));
    }
}
