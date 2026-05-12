package com.cashier.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商品销量排行")
public class SalesRankingVO {

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "总销量（可小数）")
    private BigDecimal totalQuantity;

    @Schema(description = "总销售额")
    private BigDecimal totalAmount;
}
