package com.cashier.module.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("purchase_record")
public class PurchaseRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String purchaseNo;
    private Long supplierId;
    private Long goodsId;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal totalAmount;
    private Long userId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
