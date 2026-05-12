package com.cashier.module.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
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

    /** 收银台「查询已有客户」选定后传入，优先于手机匹配，避免改姓名/电话后误建档或订单归属错 */
    @JsonAlias({"member_id"})
    @Schema(description = "已关联客户ID（可选；与 memberCardNo 二选一优先本字段）")
    private Long memberId;

    /** 离线库按手机号关联会员（member.phone）；可与会员卡号为同一串数字 */
    @JsonAlias({"member_phone"})
    @Schema(description = "会员手机号（可选；等同于旧字段 memberCardNo）")
    private String memberCardNo;

    @Min(value = 0, message = "支付方式取值非法")
    @Max(value = 4, message = "支付方式取值非法")
    @JsonAlias({"paymentType", "paymentMethod", "pay_method", "payWay", "pay_type"})
    @Schema(description = "支付方式：0-现金 1-微信 2-支付宝 3-会员余额 4-银行卡；缺省按现金")
    private Integer payType;

    @NotEmpty(message = "购物车不能为空")
    @Valid
    @Schema(description = "购物车商品列表", required = true)
    private List<SettleItemDTO> items;

    /** 兼容旧客户端：纯文本备注；优先使用 customerRemark / orderRemark */
    @Schema(description = "备注（可选；建议使用 customerRemark + orderRemark）")
    private String remark;

    @Schema(description = "客户备注（自由文本，写入订单备注区）")
    private String customerRemark;

    @Schema(description = "订单备注（自由文本）")
    private String orderRemark;

    @JsonAlias({"region_codes"})
    @Schema(description = "行政区划编码串（逗号分隔）")
    private String regionCodes;

    @Schema(description = "定价/减免说明（可选；不参与金额计算）")
    private String pricingNote;

    /** 客户姓名（与库列 customer_name 对应；可与 remark 内「客户:」重复，优先本字段） */
    @Schema(description = "客户姓名")
    private String customerName;

    /** 客户电话（与库列 customer_phone 对应；可与备注「电话:」重复，优先本字段） */
    @JsonAlias({"customer_phone", "phone"})
    @Schema(description = "客户电话")
    private String customerPhone;

    /** yyyy-MM-dd；可与备注「还款日期:」重复，优先本字段 */
    @JsonAlias({"repay_date"})
    @Schema(description = "还款日期")
    private String repayDate;

    /** yyyy-MM-dd；可与备注「送货日期:」重复，优先本字段 */
    @JsonAlias({"delivery_date"})
    @Schema(description = "送货日期")
    private String deliveryDate;

    @JsonAlias({"customer_address"})
    @Schema(description = "客户收货地址全文")
    private String customerAddress;

    @JsonAlias({"customer_gender"})
    @Schema(description = "客户性别：0-未知 1-男 2-女")
    private Integer customerGender;

    /** yyyy-MM-dd；离线库 order_date NOT NULL */
    @JsonAlias({"order_date"})
    @Schema(description = "订单日期")
    private String orderDate;

    @JsonAlias({"receivable_amount"})
    @Schema(description = "约定应收合计（与收银台「应收」一致；空则等于商品总额）")
    private BigDecimal receivableAmount;
}
