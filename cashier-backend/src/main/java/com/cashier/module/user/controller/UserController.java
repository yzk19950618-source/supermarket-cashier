package com.cashier.module.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cashier.common.dto.IdDTO;
import com.cashier.common.result.R;
import com.cashier.module.user.dto.UserDTO;
import com.cashier.module.user.dto.UserQueryDTO;
import com.cashier.module.user.dto.UserStatusDTO;
import com.cashier.module.user.service.UserService;
import com.cashier.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器
 *
 * @author cashier
 * @since 2024-01-01
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户列表")
    @PostMapping("/page")
    public R<IPage<UserVO>> page(@RequestBody UserQueryDTO queryDTO) {
        return R.ok(userService.pageList(queryDTO));
    }

    @Operation(summary = "新增用户")
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid UserDTO dto) {
        userService.addUser(dto);
        return R.ok();
    }

    @Operation(summary = "修改用户")
    @PostMapping("/update")
    public R<Void> update(@RequestBody @Valid UserDTO dto) {
        userService.updateUser(dto);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody @Valid IdDTO dto) {
        userService.removeById(dto.getId());
        return R.ok();
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/resetPwd")
    public R<Void> resetPwd(@RequestBody @Valid IdDTO dto) {
        userService.resetPassword(dto.getId());
        return R.ok();
    }

    @Operation(summary = "修改用户状态")
    @PostMapping("/updateStatus")
    public R<Void> updateStatus(@RequestBody @Valid UserStatusDTO dto) {
        userService.updateStatus(dto);
        return R.ok();
    }
}
