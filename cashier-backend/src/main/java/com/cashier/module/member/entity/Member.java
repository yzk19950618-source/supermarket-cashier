package com.cashier.module.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员实体
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@TableName("member")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会员姓名 */
    private String name;

    /** 手机号（离线库唯一键 uk_phone） */
    private String phone;

    /** 性别 */
    private Integer gender;

    /** 地址 */
    private String address;

    /** 备注 */
    private String remark;

    /** 余额 */
    private BigDecimal balance;

    /** 积分 */
    private Integer points;

    /** 折扣 */
    private BigDecimal discount;

    /** 状态 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
