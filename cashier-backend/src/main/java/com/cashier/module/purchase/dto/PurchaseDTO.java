package com.cashier.module.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "进货信息")
public class PurchaseDTO {

    @NotNull(message = "供应商不能为空")
    @Schema(description = "供应商ID", required = true)
    private Long supplierId;

    @NotNull(message = "商品不能为空")
    @Schema(description = "商品ID", required = true)
    private Long goodsId;

    @NotNull(message = "进货数量不能为空")
    @Min(value = 1, message = "进货数量至少为1")
    @Schema(description = "进货数量", required = true)
    private Integer quantity;

    @NotNull(message = "进货单价不能为空")
    @Schema(description = "进货单价", required = true)
    private BigDecimal purchasePrice;

    @Schema(description = "备注")
    private String remark;
}
