package com.cashier.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cashier.module.order.entity.SaleOrder;
import com.cashier.module.order.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 销售订单 Mapper
 *
 * @author cashier
 * @since 2024-01-01
 */
@Mapper
public interface SaleOrderMapper extends BaseMapper<SaleOrder> {

    /**
     * 分页查询订单列表（关联用户和会员信息）
     */
    IPage<OrderVO> selectPageVO(Page<OrderVO> page,
                                @Param("orderNo") String orderNo,
                                @Param("payType") Integer payType,
                                @Param("status") Integer status,
                                @Param("startDate") String startDate,
                                @Param("endDate") String endDate,
                                @Param("memberId") Long memberId);
}
