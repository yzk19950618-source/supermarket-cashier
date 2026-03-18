package com.cashier.module.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "供应商信息")
public class SupplierDTO {

    @Schema(description = "供应商ID（编辑时必传）")
    private Long id;

    @NotBlank(message = "供应商名称不能为空")
    @Schema(description = "供应商名称", required = true)
    private String name;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "地址")
    private String address;
}
