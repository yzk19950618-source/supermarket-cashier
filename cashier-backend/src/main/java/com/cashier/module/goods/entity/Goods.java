package com.cashier.module.goods.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@TableName("goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品条码（与离线库 goods.barcode、uk_barcode 一致） */
    private String barcode;

    /** 商品名称 */
    private String name;

    /** 名称首字母（离线库 name_initial） */
    private String nameInitial;

    /** 分类ID */
    private Long categoryId;

    /** 单位 */
    private String unit;

    /** 进货价 */
    private BigDecimal purchasePrice;

    /** 零售价 */
    private BigDecimal sellingPrice;

    /** 库存数量 */
    private Integer stock;

    /** 库存预警值 */
    private Integer stockWarning;

    /** 商品图片 */
    private String image;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
