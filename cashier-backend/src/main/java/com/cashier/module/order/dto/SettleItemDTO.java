package com.cashier.module.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "结算商品项")
public class SettleItemDTO {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", required = true)
    private Long goodsId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "购买数量至少为 1")
    @Schema(description = "购买数量（整数袋）", required = true)
    private Integer quantity;

    @JsonAlias({"promo_enabled"})
    @Schema(description = "本行是否参与同款买满送（商品需在后台开启活动）")
    private Boolean promoEnabled;

    @Positive(message = "买满件数至少为 1")
    @Schema(description = "活动覆盖：买满件数（空则用商品配置，整数）")
    private Integer promoBuyQty;

    @DecimalMin(value = "0.001", inclusive = true, message = "赠送数量须大于 0")
    @Digits(integer = 9, fraction = 3, message = "赠送数量最多三位小数")
    @Schema(description = "活动覆盖：赠送数量（空则用商品配置，可小数，如 0.5 袋）")
    private BigDecimal promoGiftQty;
}
