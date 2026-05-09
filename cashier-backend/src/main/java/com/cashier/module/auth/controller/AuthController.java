package com.cashier.module.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.cashier.common.constant.CommonConstant;
import com.cashier.common.exception.BusinessException;
import com.cashier.common.result.R;
import com.cashier.common.result.ResultCode;
import com.cashier.module.auth.dto.LoginDTO;
import com.cashier.module.auth.dto.UpdatePwdDTO;
import com.cashier.module.auth.vo.LoginVO;
import com.cashier.module.user.entity.User;
import com.cashier.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * 处理登录、登出、获取用户信息、修改密码
 *
 * @author cashier
 * @since 2024-01-01
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping(value = "/api/auth", consumes = MediaType.ALL_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     *
     * @param dto 登录请求（用户名 + 密码）
     * @return Token 和用户信息
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        // 1. 根据用户名查询用户
        User user = userService.getByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 2. 校验密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 3. 校验账号状态
        if (user.getStatus() == CommonConstant.STATUS_DISABLED) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 4. Sa-Token 登录
        StpUtil.login(user.getId());

        // 5. 构建响应
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());

        LoginVO.UserInfoVO userInfoVO = new LoginVO.UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setRealName(user.getRealName());
        userInfoVO.setPhone(user.getPhone());
        userInfoVO.setRole(user.getRole());
        userInfoVO.setAvatar(user.getAvatar());
        loginVO.setUserInfo(userInfoVO);

        return R.ok(loginVO);
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @Operation(summary = "获取当前登录用户信息")
    @PostMapping("/info")
    public R<LoginVO.UserInfoVO> info() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        LoginVO.UserInfoVO vo = new LoginVO.UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setAvatar(user.getAvatar());

        return R.ok(vo);
    }

    /**
     * 修改密码
     *
     * @param dto 修改密码请求（原密码 + 新密码）
     */
    @Operation(summary = "修改密码")
    @PostMapping("/updatePwd")
    public R<Void> updatePwd(@RequestBody @Valid UpdatePwdDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 校验原密码
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        // 更新密码
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userService.updateById(updateUser);

        // 退出登录，让用户重新登录
        StpUtil.logout();

        return R.ok();
    }
}
