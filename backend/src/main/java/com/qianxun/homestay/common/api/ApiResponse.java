package com.qianxun.homestay.common.api;

/**
 * 统一 API 响应结构，供小程序和管理后台共享。
 *
 * @param <T> 响应数据类型
 */
public record ApiResponse<T>(
        /** 业务状态码，成功固定为 200。 */
        int code,
        /** 面向调用方的简短消息。 */
        String message,
        /** 当前接口的数据；失败时为空。 */
        T data) {

    /**
     * 创建成功响应。
     *
     * @param data 成功数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 创建错误响应。
     *
     * @param code HTTP 状态码
     * @param message 可读错误消息
     * @return 错误响应
     */
    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
