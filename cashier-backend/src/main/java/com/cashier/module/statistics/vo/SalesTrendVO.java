package com.cashier.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "销售趋势数据")
public class SalesTrendVO {

    @Schema(description = "日期列表")
    private List<String> dates;

    @Schema(description = "销售额列表")
    private List<BigDecimal> amounts;
}
