package com.cashier.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
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

        @Schema(description = "销售单价")
        private BigDecimal sellingPrice;

        @Schema(description = "购买数量")
        private Integer quantity;

        @Schema(description = "小计金额")
        private BigDecimal subtotal;
    }
}
