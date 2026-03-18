package com.cashier.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户新增/编辑 DTO
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "用户信息")
public class UserDTO {

    @Schema(description = "用户ID（编辑时必传）")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", required = true)
    private String username;

    @Schema(description = "密码（新增时必传）")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", required = true)
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @NotNull(message = "角色不能为空")
    @Schema(description = "角色：0-收银员 1-管理员", required = true)
    private Integer role;
}
