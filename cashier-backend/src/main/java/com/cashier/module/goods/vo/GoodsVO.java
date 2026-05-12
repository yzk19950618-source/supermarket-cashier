package com.cashier.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息 VO
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "商品信息")
public class GoodsVO {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品条码")
    private String barcode;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "名称首字母（拼音简写等，用于首字母搜索）")
    private String nameInitial;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "进货价")
    private BigDecimal purchasePrice;

    @Schema(description = "零售价")
    private BigDecimal sellingPrice;

    @Schema(description = "库存数量（整数袋）")
    private Integer stock;

    @Schema(description = "库存预警值（整数）")
    private Integer stockWarning;

    @Schema(description = "商品图片URL")
    private String image;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "同款买赠：1 启用")
    private Integer promoEnabled;

    @Schema(description = "满多少袋触发（整数）")
    private Integer promoBuyQty;

    @Schema(description = "送多少同款（可小数袋）")
    private BigDecimal promoGiftQty;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
