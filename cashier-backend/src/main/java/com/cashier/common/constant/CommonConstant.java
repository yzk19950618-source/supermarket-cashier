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
     * 订单状态：已退款（与离线库 init.sql 注释一致）
     */
    public static final int ORDER_STATUS_REFUNDED = 0;

    /**
     * 订单状态：已支付（离线库文案「已支付」） / 业务侧等价「已完成」
     */
    public static final int ORDER_STATUS_PAID = 1;

    /** @deprecated 请优先使用 {@link #ORDER_STATUS_PAID} */
    @Deprecated
    public static final int ORDER_STATUS_COMPLETED = ORDER_STATUS_PAID;

    /**
     * 订单状态：未支付（离线库 sale_order.status 默认值）
     */
    public static final int ORDER_STATUS_UNPAID = 2;

    /**
     * 会员备注（member.remark）最大字符数；与 init.sql / incremental/003 一致
     */
    public static final int MEMBER_REMARK_MAX_LENGTH = 2000;
}
