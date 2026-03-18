package com.cashier.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求 DTO
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "修改密码请求")
public class UpdatePwdDTO {

    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码", required = true)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度为6-20位")
    @Schema(description = "新密码", required = true)
    private String newPassword;
}
