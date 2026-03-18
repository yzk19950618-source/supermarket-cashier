package com.cashier.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * @param <T> 数据泛型
 * @author cashier
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应结果")
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码")
    private int code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳")
    private long timestamp;

    /**
     * 成功响应（无数据）
     */
    public static <T> R<T> ok() {
        return new R<>(200, "操作成功", null, System.currentTimeMillis());
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data, System.currentTimeMillis());
    }

    /**
     * 成功响应（自定义消息 + 数据）
     */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data, System.currentTimeMillis());
    }

    /**
     * 失败响应
     *
     * @param message 错误信息
     */
    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null, System.currentTimeMillis());
    }

    /**
     * 失败响应（自定义状态码）
     *
     * @param code    状态码
     * @param message 错误信息
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null, System.currentTimeMillis());
    }

    /**
     * 失败响应（使用枚举）
     */
    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null, System.currentTimeMillis());
    }
}
