package com.cashier.module.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "会员信息")
public class MemberDTO {

    @Schema(description = "会员ID（编辑时必传）")
    private Long id;

    @Schema(description = "会员卡号（已废弃：离线库无此列；与手机号一致时可不传）")
    private String cardNo;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名", required = true)
    private String name;

    @Schema(description = "手机号（可选；空则后台生成占位号）")
    private String phone;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender = 0;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "备注")
    private String remark;
}
