package com.cashier.common.constant;

/**
 * 公共常量
 *
 * @author cashier
 * @since 2024-01-01
 */
public class CommonConstant {

    /**
     * 默认密码
     */
    public static final String DEFAULT_PASSWORD = "123456";

    /**
     * 状态：启用
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 状态：禁用
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 支付方式：现金
     */
    public static final int PAY_TYPE_CASH = 0;

    /**
     * 支付方式：微信
     */
    public static final int PAY_TYPE_WECHAT = 1;

    /**
     * 支付方式：支付宝
     */
    public static final int PAY_TYPE_ALIPAY = 2;

    /**
     * 支付方式：会员余额
     */
    public static final int PAY_TYPE_BALANCE = 3;

    /**
     * 支付方式：银行卡
     */
    public static final int PAY_TYPE_BANK = 4;

    /**
     * 订单状态：已退款
     */
    public static final int ORDER_STATUS_REFUNDED = 0;

    /**
     * 订单状态：已完成
     */
    public static final int ORDER_STATUS_COMPLETED = 1;
}
