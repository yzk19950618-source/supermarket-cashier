package com.cashier.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录成功响应 VO
 *
 * @author cashier
 * @since 2024-01-01
 */
@Data
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "认证Token")
    private String token;

    @Schema(description = "用户信息")
    private UserInfoVO userInfo;

    /**
     * 登录用户信息
     */
    @Data
    @Schema(description = "用户信息")
    public static class UserInfoVO {

        @Schema(description = "用户ID")
        private Long id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "真实姓名")
        private String realName;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "角色：0-收银员 1-管理员")
        private Integer role;

        @Schema(description = "头像URL")
        private String avatar;
    }
}
