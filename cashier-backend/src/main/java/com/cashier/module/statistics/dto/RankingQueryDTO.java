package com.cashier.module.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "排行查询条件")
public class RankingQueryDTO {

    @Schema(description = "排行数量", example = "10")
    private Integer top = 10;

    @Schema(description = "天数")
    private Integer days = 30;
}
