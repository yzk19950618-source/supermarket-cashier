package com.cashier.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cashier.module.order.dto.OrderQueryDTO;
import com.cashier.module.order.dto.SettleDTO;
import com.cashier.module.order.vo.OrderDetailVO;
import com.cashier.module.order.vo.OrderVO;
import com.cashier.module.order.vo.TodaySummaryVO;

public interface OrderService {

    /**
     * 收银结算（核心方法）
     *
     * @param dto    结算请求
     * @param userId 当前收银员ID
     * @return 订单信息
     */
    OrderVO settle(SettleDTO dto, Long userId);

    /**
     * 分页查询订单
     */
    IPage<OrderVO> pageList(OrderQueryDTO queryDTO);

    /**
     * 查询订单详情
     */
    OrderDetailVO getDetail(Long id);

    /**
     * 订单退款
     */
    void refund(Long id);

    /**
     * 今日订单汇总
     */
    TodaySummaryVO todaySummary();
}
