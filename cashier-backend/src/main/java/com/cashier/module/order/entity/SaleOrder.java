package com.cashier.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单实体
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@TableName("sale_order")
public class SaleOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 会员ID */
    private Long memberId;

    /** 收银员ID */
    private Long userId;

    /** 客户地址（库 customer_address） */
    private String customerAddress;

    /** 客户性别（库 customer_gender） */
    private Integer customerGender;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal realAmount;

    /** 支付方式 */
    private Integer payType;

    /** 订单状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 客户姓名（库表 customer_name；会员单取会员名，散客取备注「客户:」段） */
    private String customerName;

    /** 客户电话（库表 customer_phone） */
    private String customerPhone;

    /** 还款日期（库表 repay_date） */
    private LocalDate repayDate;

    /** 送货日期（库表 delivery_date） */
    private LocalDate deliveryDate;

    /** 订单业务日期（库 order_date） */
    private LocalDate orderDate;

    /** 核销/支付时间（库 paid_time） */
    private LocalDateTime paidTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
