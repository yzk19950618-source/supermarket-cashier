package com.cashier.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cashier.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 *
 * @author cashier
 * @since 2024-01-01
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
