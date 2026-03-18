package com.cashier.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cashier.module.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 扣减会员余额
     */
    @Update("UPDATE member SET balance = balance - #{amount} WHERE id = #{id} AND balance >= #{amount} AND deleted = 0")
    int deductBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 增加会员余额
     */
    @Update("UPDATE member SET balance = balance + #{amount} WHERE id = #{id} AND deleted = 0")
    int addBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 增加积分
     */
    @Update("UPDATE member SET points = points + #{points} WHERE id = #{id} AND deleted = 0")
    int addPoints(@Param("id") Long id, @Param("points") Integer points);

    /**
     * 扣减积分
     */
    @Update("UPDATE member SET points = GREATEST(points - #{points}, 0) WHERE id = #{id} AND deleted = 0")
    int deductPoints(@Param("id") Long id, @Param("points") Integer points);
}
