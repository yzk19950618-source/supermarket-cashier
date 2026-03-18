package com.cashier.module.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "会员信息")
public class MemberDTO {

    @Schema(description = "会员ID（编辑时必传）")
    private Long id;

    @NotBlank(message = "会员卡号不能为空")
    @Schema(description = "会员卡号", required = true)
    private String cardNo;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名", required = true)
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号", required = true)
    private String phone;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender = 0;

    @Schema(description = "折扣")
    private BigDecimal discount = BigDecimal.ONE;
}
