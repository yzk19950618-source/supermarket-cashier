package com.cashier.module.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "进货查询条件")
public class PurchaseQueryDTO {

    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "进货单号")
    private String purchaseNo;

    @Schema(description = "供应商ID")
    private Long supplierId;
}
