package com.cashier.module.region.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 行政区划表 {@code region}（与离线 region-init.sql 一致）。
 */
@Data
@TableName("region")
public class Region {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    @TableField("parent_code")
    private String parentCode;

    private Integer level;

    private Integer sort;
}
