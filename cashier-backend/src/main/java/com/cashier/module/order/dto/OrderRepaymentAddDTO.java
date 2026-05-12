package com.cashier.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "新增还款")
public class OrderRepaymentAddDTO {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", required = true)
    private Long orderId;

    @NotNull(message = "还款金额不能为空")
    @DecimalMin(value = "0.01", message = "还款金额须大于0")
    @Schema(description = "还款金额", required = true)
    private BigDecimal amount;

    @NotNull(message = "支付方式不能为空")
    @Schema(description = "支付方式：0-现金 1-微信 2-支付宝 4-银行卡（不含余额）", required = true)
    private Integer payType;

    @Schema(description = "备注")
    private String remark;
}
