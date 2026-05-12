package com.cashier.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单信息")
public class OrderVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员名称")
    private String memberName;

    @Schema(description = "收银员ID")
    private Long userId;

    @Schema(description = "收银员名称")
    private String userName;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "累计实收（已还金额），对应库字段 real_amount；分批还款累加至此")
    private BigDecimal realAmount;

    @Schema(description = "应收合计（优惠后）= totalAmount − discountAmount；与分批还款上限一致")
    private BigDecimal receivableAmount;

    @Schema(description = "累计已还（与 realAmount 同义，便于老前端只认 paid 语义时读取）")
    private BigDecimal paidAmount;

    @Schema(description = "剩余欠款 = receivableAmount − paidAmount；老前端请勿再用 totalAmount−realAmount 计算（有优惠时与真实欠款不一致）")
    private BigDecimal remainDebt;

    @Schema(description = "支付方式")
    private Integer payType;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "客户姓名")
    private String customerName;

    @Schema(description = "客户电话")
    private String customerPhone;

    @Schema(description = "还款日期")
    private LocalDate repayDate;

    @Schema(description = "送货日期")
    private LocalDate deliveryDate;

    @Schema(description = "客户地址")
    private String customerAddress;

    @Schema(description = "客户性别")
    private Integer customerGender;

    @Schema(description = "订单日期")
    private LocalDate orderDate;

    @Schema(description = "核销时间")
    private LocalDateTime paidTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
