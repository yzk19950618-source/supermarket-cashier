package com.cashier.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "今日订单汇总")
public class TodaySummaryVO {

    @Schema(description = "今日订单数")
    private Integer orderCount;

    @Schema(description = "今日销售额")
    private BigDecimal totalSales;

    @Schema(description = "今日退款数")
    private Integer refundCount;

    @Schema(description = "今日退款额")
    private BigDecimal refundAmount;
}
