package com.cashier.module.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "趋势查询条件")
public class TrendQueryDTO {

    @Schema(description = "天数（7/30/90）", example = "7")
    private Integer days = 7;
}
