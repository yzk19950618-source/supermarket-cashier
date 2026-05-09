package com.cashier.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author cashier
 * @since 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // 认证相关 4xx
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),
    USERNAME_OR_PASSWORD_ERROR(4001, "用户名或密码错误"),
    ACCOUNT_DISABLED(4002, "账号已被禁用"),
    OLD_PASSWORD_ERROR(4003, "原密码错误"),

    // 参数相关
    PARAM_ERROR(4010, "参数校验失败"),
    /** 未匹配到接口处理器（常见于误走静态资源、HTTP 方法或路径不对） */
    HTTP_METHOD_NOT_ALLOWED(4050, "请求方式不允许"),
    UNSUPPORTED_MEDIA_TYPE(4150, "不支持的媒体类型"),

    DATA_NOT_FOUND(4011, "数据不存在"),
    DATA_ALREADY_EXISTS(4012, "数据已存在"),

    // 业务相关 5xxx
    STOCK_NOT_ENOUGH(5001, "库存不足"),
    MEMBER_BALANCE_NOT_ENOUGH(5002, "会员余额不足"),
    ORDER_ALREADY_REFUNDED(5003, "订单已退款，不可重复操作"),
    GOODS_OFF_SHELF(5004, "商品已下架");

    private final int code;
    private final String message;
}
