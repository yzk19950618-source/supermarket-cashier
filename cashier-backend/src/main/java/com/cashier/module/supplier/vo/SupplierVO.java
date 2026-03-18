package com.cashier.module.supplier.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "供应商信息")
public class SupplierVO {

    private Long id;
    private String name;
    private String contact;
    private String phone;
    private String address;
    private Integer status;
    private LocalDateTime createTime;
}
