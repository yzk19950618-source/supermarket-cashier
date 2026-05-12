package com.cashier.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品新增/编辑 DTO
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "商品信息")
public class GoodsDTO {

    @Schema(description = "商品ID（编辑时必传）")
    private Long id;

    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称", required = true)
    private String name;

    @NotNull(message = "分类不能为空")
    @Schema(description = "分类ID", required = true)
    private Long categoryId;

    @Schema(description = "单位", example = "个")
    private String unit = "个";

    @NotNull(message = "进货价不能为空")
    @Schema(description = "进货价", required = true)
    private BigDecimal purchasePrice;

    @NotNull(message = "零售价不能为空")
    @Schema(description = "零售价", required = true)
    private BigDecimal sellingPrice;

    @Schema(description = "库存数量（整数袋；后台内部可含小数扣减）")
    private Integer stock = 0;

    @Schema(description = "库存预警值（整数）")
    private Integer stockWarning = 10;

    @Schema(description = "商品图片URL")
    private String image;

    @Schema(description = "同款买赠：1 启用")
    private Integer promoEnabled;

    @Schema(description = "满多少袋触发（买满，整数）")
    private Integer promoBuyQty;

    @Schema(description = "送多少同款（可小数袋）")
    private BigDecimal promoGiftQty;
}
