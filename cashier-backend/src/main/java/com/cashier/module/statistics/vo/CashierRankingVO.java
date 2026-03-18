package com.cashier.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "收银员业绩排行")
public class CashierRankingVO {

    @Schema(description = "收银员姓名")
    private String userName;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "总销售额")
    private BigDecimal totalAmount;

    @Schema(description = "平均客单价")
    private BigDecimal avgPrice;
}
