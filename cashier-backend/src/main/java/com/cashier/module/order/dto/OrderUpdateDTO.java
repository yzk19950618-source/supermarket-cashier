package com.cashier.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "编辑订单（结构化字段）")
public class OrderUpdateDTO {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", required = true)
    private Long id;

    @Schema(description = "实付/已收金额（累计）；若同时传 remainDebt 则以欠款调整为准")
    private BigDecimal realAmount;

    /**
     * 手动调整后的剩余欠款；服务端按「订单总额 − 还款合计 − 目标欠款」反算优惠金额，
     * 并把 real_amount 与还款明细总和对齐。
     */
    @Schema(description = "剩余欠款（手动调整；与 realAmount 二选一优先本字段）")
    private BigDecimal remainDebt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "客户姓名")
    private String customerName;

    @Schema(description = "客户电话")
    private String customerPhone;

    @Schema(description = "客户地址")
    private String customerAddress;

    @Schema(description = "还款日期 yyyy-MM-dd")
    private String repayDate;

    @Schema(description = "送货日期 yyyy-MM-dd")
    private String deliveryDate;

    @Schema(description = "订单日期 yyyy-MM-dd")
    private String orderDate;
}
