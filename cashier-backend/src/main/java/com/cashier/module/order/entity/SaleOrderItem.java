package com.cashier.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@TableName("sale_order_item")
public class SaleOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 商品ID */
    private Long goodsId;

    /** 商品名称（冗余） */
    private String goodsName;

    /** 商品条码（冗余） */
    private String barcode;

    /** 商品品类名称（冗余，库 category_name） */
    private String categoryName;

    /** 销售单价 */
    private BigDecimal sellingPrice;

    /** 购买数量（可小数） */
    private BigDecimal quantity;

    /** 小计金额 */
    private BigDecimal subtotal;

    /** 1=活动赠品（同款买赠） */
    private Integer isGift;

    /** 创建时间 */
    private LocalDateTime createTime;
}
