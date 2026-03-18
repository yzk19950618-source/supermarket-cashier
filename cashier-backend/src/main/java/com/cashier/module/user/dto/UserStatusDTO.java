package com.cashier.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户状态修改 DTO
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "用户状态修改")
public class UserStatusDTO {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", required = true)
    private Long id;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：0-禁用 1-启用", required = true)
    private Integer status;
}
