package com.cashier.module.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "供应商查询条件")
public class SupplierQueryDTO {

    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "供应商名称")
    private String name;

    @Schema(description = "联系人")
    private String contact;
}
