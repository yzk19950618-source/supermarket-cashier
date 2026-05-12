package com.cashier.module.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "会员卡号查询请求")
public class MemberCardDTO {

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号（离线库 member.uk_phone；前端仍可沿用字段名 cardNo）", required = true)
    private String cardNo;
}
