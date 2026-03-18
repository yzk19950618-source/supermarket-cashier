package com.cashier.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cashier.module.user.dto.UserDTO;
import com.cashier.module.user.dto.UserQueryDTO;
import com.cashier.module.user.dto.UserStatusDTO;
import com.cashier.module.user.entity.User;
import com.cashier.module.user.vo.UserVO;

/**
 * 用户服务接口
 *
 * @author cashier
 * @since 2024-01-01
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    User getByUsername(String username);

    /**
     * 分页查询用户
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<UserVO> pageList(UserQueryDTO queryDTO);

    /**
     * 新增用户
     *
     * @param dto 用户信息
     */
    void addUser(UserDTO dto);

    /**
     * 修改用户
     *
     * @param dto 用户信息
     */
    void updateUser(UserDTO dto);

    /**
     * 重置用户密码为默认密码
     *
     * @param id 用户ID
     */
    void resetPassword(Long id);

    /**
     * 修改用户状态
     *
     * @param dto 状态修改请求
     */
    void updateStatus(UserStatusDTO dto);
}
