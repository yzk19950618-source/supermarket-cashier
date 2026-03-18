package com.cashier.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "首页看板数据")
public class DashboardVO {

    @Schema(description = "今日销售额")
    private BigDecimal todaySales;

    @Schema(description = "今日订单数")
    private Integer todayOrders;

    @Schema(description = "今日客单价")
    private BigDecimal todayAvgPrice;

    @Schema(description = "本月销售额")
    private BigDecimal monthSales;
}
