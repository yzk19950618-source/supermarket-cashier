package com.cashier.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 收银结算请求 DTO
 * 核心结算接口的请求参数
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "结算请求")
public class SettleDTO {

    @Schema(description = "会员卡号（可选，散客不传）")
    private String memberCardNo;

    @NotNull(message = "支付方式不能为空")
    @Schema(description = "支付方式：0-现金 1-微信 2-支付宝 3-会员余额 4-银行卡", required = true)
    private Integer payType;

    @NotEmpty(message = "购物车不能为空")
    @Valid
    @Schema(description = "购物车商品列表", required = true)
    private List<SettleItemDTO> items;

    @Schema(description = "备注")
    private String remark;
}
