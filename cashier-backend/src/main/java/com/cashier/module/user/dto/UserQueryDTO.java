package com.cashier.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询条件 DTO
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "用户查询条件")
public class UserQueryDTO {

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "用户名（模糊搜索）")
    private String username;

    @Schema(description = "真实姓名（模糊搜索）")
    private String realName;

    @Schema(description = "角色筛选")
    private Integer role;

    @Schema(description = "状态筛选")
    private Integer status;
}
