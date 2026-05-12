package com.cashier.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "首页看板数据")
public class DashboardVO {

    @Schema(description = "今日订单数（当日创建，含已退款）")
    private Integer todayOrderCount;

    @Schema(description = "今日总营业额（当日创建且非退款订单的商品总额合计）")
    private BigDecimal todayTotalTurnover;

    @Schema(description = "今日新增待收款（当日创建且非退款订单的剩余欠款合计）")
    private BigDecimal todayPendingCollection;

    @Schema(description = "本年订单数（自然年按创建时间，含已退款）")
    private Integer yearOrderCount;

    @Schema(description = "本年总已收款金额（还款记录发生在本年 + 本年创建当场结清且无还款明细的实收）")
    private BigDecimal yearTotalCollectedAmount;

    @Schema(description = "本年总未收款金额（本年创建且非退款订单的剩余欠款合计）")
    private BigDecimal yearTotalUncollectedAmount;

    @Schema(description = "本年总金额（本年创建且非退款订单的商品总额合计）")
    private BigDecimal yearTotalAmount;
}
