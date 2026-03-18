package com.cashier.module.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "会员信息")
public class MemberVO {

    @Schema(description = "会员ID")
    private Long id;

    @Schema(description = "会员卡号")
    private String cardNo;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别")
    private Integer gender;

    @Schema(description = "余额")
    private BigDecimal balance;

    @Schema(description = "积分")
    private Integer points;

    @Schema(description = "折扣")
    private BigDecimal discount;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
