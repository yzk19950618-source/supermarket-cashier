package com.cashier.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单还款记录")
public class OrderRepaymentVO {

    @Schema(description = "还款记录 ID")
    private Long id;

    @Schema(description = "还款金额")
    private BigDecimal amount;

    @Schema(description = "支付方式：0-现金 1-微信 2-支付宝 3-会员余额 4-银行卡")
    private Integer payType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "还款时间")
    private LocalDateTime createTime;

    @Schema(description = "操作人")
    private String operatorName;
}
