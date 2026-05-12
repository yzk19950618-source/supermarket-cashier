package com.cashier.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sale_order_attachment")
public class SaleOrderAttachment {

    /** 发票 */
    public static final int TYPE_INVOICE = 1;
    /** 欠条 */
    public static final int TYPE_IOU = 2;
    /** 送货图片 */
    public static final int TYPE_DELIVERY_IMAGE = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 1-发票 2-欠条 3-送货图片 */
    private Integer attachmentType;

    private String url;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
