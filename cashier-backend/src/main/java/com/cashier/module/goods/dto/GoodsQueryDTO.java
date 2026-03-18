package com.cashier.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品查询条件 DTO
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "商品查询条件")
public class GoodsQueryDTO {

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "商品名称（模糊搜索）")
    private String name;

    @Schema(description = "商品条码（精确搜索）")
    private String barcode;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态：0-下架 1-上架")
    private Integer status;
}
