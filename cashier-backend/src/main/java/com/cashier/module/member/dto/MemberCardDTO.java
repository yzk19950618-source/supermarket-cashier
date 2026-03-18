package com.cashier.module.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "会员卡号查询请求")
public class MemberCardDTO {

    @NotBlank(message = "会员卡号不能为空")
    @Schema(description = "会员卡号", required = true)
    private String cardNo;
}
