package com.cashier.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "条码查询请求")
public class BarcodeDTO {

    @NotBlank(message = "条码不能为空")
    @Schema(description = "商品条码", required = true)
    private String barcode;
}
