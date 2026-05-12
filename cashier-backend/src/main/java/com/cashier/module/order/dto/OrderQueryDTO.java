package com.cashier.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单查询条件")
public class OrderQueryDTO {

    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "支付方式")
    private Integer payType;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "开始日期（yyyy-MM-dd）")
    private String startDate;

    @Schema(description = "结束日期（yyyy-MM-dd）")
    private String endDate;

    @Schema(description = "会员/客户ID（筛选该客户全部订单）")
    private Long memberId;
}
