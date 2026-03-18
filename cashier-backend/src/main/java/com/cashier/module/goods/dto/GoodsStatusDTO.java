package com.cashier.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "商品状态修改")
public class GoodsStatusDTO {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", required = true)
    private Long id;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：0-下架 1-上架", required = true)
    private Integer status;
}
