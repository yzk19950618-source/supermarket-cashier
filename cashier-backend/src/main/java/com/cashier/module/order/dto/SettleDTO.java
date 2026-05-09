package com.cashier.module.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
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

    @Schema(description = "会员卡号（可选，散客不传）")
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

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "收货详细地址（可与省市区文案拼接）")
    private String receiverAddress;

    @Schema(description = "省市区编码，逗号分隔，如 110000,110100,110101")
    private String receiverRegionCodes;

    @Schema(description = "附件图片 URL 列表（相对路径如 /uploads/... 或完整 URL）")
    private List<String> attachmentUrls = new ArrayList<>();
}
