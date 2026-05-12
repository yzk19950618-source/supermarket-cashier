package com.cashier.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单详情 VO（含明细列表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单详情")
public class OrderDetailVO extends OrderVO {

    @Schema(description = "订单明细列表")
    private List<OrderItemVO> items;

    @Schema(description = "还款记录（时间倒序）")
    private List<OrderRepaymentVO> repayments = new ArrayList<>();

    @Schema(description = "订单附件（发票、欠条、送货图片等）")
    private List<OrderAttachmentVO> attachments = new ArrayList<>();

    @Schema(description = "发票类附件 URL（仅 type=1；兼容旧客户端）")
    private List<String> invoiceUrls = new ArrayList<>();

    /**
     * 订单明细项
     */
    @Data
    @Schema(description = "订单明细项")
    public static class OrderItemVO {

        @Schema(description = "明细ID")
        private Long id;

        @Schema(description = "商品ID")
        private Long goodsId;

        @Schema(description = "商品名称")
        private String goodsName;

        @Schema(description = "商品条码")
        private String barcode;

        @Schema(description = "品类名称")
        private String categoryName;

        @Schema(description = "销售单价")
        private BigDecimal sellingPrice;

        @Schema(description = "购买数量（可小数）")
        private BigDecimal quantity;

        @Schema(description = "小计金额")
        private BigDecimal subtotal;

        @Schema(description = "1=活动赠品")
        private Integer isGift;
    }

    /**
     * 订单附件项
     */
    @Data
    @Schema(description = "订单附件")
    public static class OrderAttachmentVO {

        @Schema(description = "附件记录 ID")
        private Long id;

        @Schema(description = "类型：1-发票 2-欠条 3-送货图片")
        private Integer attachmentType;

        @Schema(description = "图片 URL")
        private String url;
    }
}
