package com.cashier.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "结算商品项")
public class SettleItemDTO {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", required = true)
    private Long goodsId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Schema(description = "购买数量", required = true)
    private Integer quantity;
}
