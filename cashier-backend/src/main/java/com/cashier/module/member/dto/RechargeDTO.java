package com.cashier.module.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "会员充值请求")
public class RechargeDTO {

    @NotNull(message = "会员ID不能为空")
    @Schema(description = "会员ID", required = true)
    private Long id;

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    @Schema(description = "充值金额", required = true)
    private BigDecimal amount;
}
