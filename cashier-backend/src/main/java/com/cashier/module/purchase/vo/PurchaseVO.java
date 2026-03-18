package com.cashier.module.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "进货记录")
public class PurchaseVO {

    private Long id;
    private String purchaseNo;
    private Long supplierId;
    private String supplierName;
    private Long goodsId;
    private String goodsName;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal totalAmount;
    private String remark;
    private LocalDateTime createTime;
}
