package com.cashier.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
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

    /** 收货详细地址（展示文案） */
    private String receiverAddress;

    /** 省市区编码，逗号分隔 */
    private String receiverRegionCodes;

    /** 附件图片 URL 列表（JSON 数组字符串） */
    private String attachmentUrls;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
