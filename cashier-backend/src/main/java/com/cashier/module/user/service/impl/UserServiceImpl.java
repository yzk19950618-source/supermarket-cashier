package com.cashier.module.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cashier.common.constant.CommonConstant;
import com.cashier.common.exception.BusinessException;
import com.cashier.common.result.ResultCode;
import com.cashier.module.user.dto.UserDTO;
import com.cashier.module.user.dto.UserQueryDTO;
import com.cashier.module.user.dto.UserStatusDTO;
import com.cashier.module.user.entity.User;
import com.cashier.module.user.mapper.UserMapper;
import com.cashier.module.user.service.UserService;
import com.cashier.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 *
 * @author cashier
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsername(String username) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }

    @Override
    public IPage<UserVO> pageList(UserQueryDTO queryDTO) {
        Page<User> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getUsername()), User::getUsername, queryDTO.getUsername())
                .like(StrUtil.isNotBlank(queryDTO.getRealName()), User::getRealName, queryDTO.getRealName())
                .eq(queryDTO.getRole() != null, User::getRole, queryDTO.getRole())
                .eq(queryDTO.getStatus() != null, User::getStatus, queryDTO.getStatus())
                .orderByDesc(User::getCreateTime);

        Page<User> userPage = baseMapper.selectPage(page, wrapper);

        // 转换为 VO
        return userPage.convert(user -> {
            UserVO vo = BeanUtil.copyProperties(user, UserVO.class);
            return vo;
        });
    }

    @Override
    public void addUser(UserDTO dto) {
        // 检查用户名是否已存在
        User existing = getByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS.getCode(), "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        // 加密密码
        String password = StrUtil.isNotBlank(dto.getPassword()) ? dto.getPassword() : CommonConstant.DEFAULT_PASSWORD;
        user.setPassword(BCrypt.hashpw(password));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setStatus(CommonConstant.STATUS_ENABLED);

        save(user);
    }

    @Override
    public void updateUser(UserDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("用户ID不能为空");
        }

        User user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());

        // 如果传了密码则更新密码
        if (StrUtil.isNotBlank(dto.getPassword())) {
            user.setPassword(BCrypt.hashpw(dto.getPassword()));
        }

        updateById(user);
    }

    @Override
    public void resetPassword(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setPassword(BCrypt.hashpw(CommonConstant.DEFAULT_PASSWORD));
        updateById(updateUser);
    }

    @Override
    public void updateStatus(UserStatusDTO dto) {
        User user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        User updateUser = new User();
        updateUser.setId(dto.getId());
        updateUser.setStatus(dto.getStatus());
        updateById(updateUser);
    }
}
