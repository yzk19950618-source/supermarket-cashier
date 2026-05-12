package com.cashier.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cashier.module.order.dto.OrderAttachmentAddDTO;
import com.cashier.module.order.dto.OrderQueryDTO;
import com.cashier.module.order.dto.OrderRepaymentAddDTO;
import com.cashier.module.order.dto.OrderUpdateDTO;
import com.cashier.module.order.dto.SettleDTO;
import com.cashier.module.order.vo.OrderDetailVO;
import com.cashier.module.order.vo.OrderVO;
import com.cashier.module.order.vo.TodaySummaryVO;

import java.util.List;

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
     * 按与分页相同的筛选条件导出（最多 2 万条），用于 Excel / 图片渲染
     */
    List<OrderVO> exportRows(OrderQueryDTO queryDTO);

    /**
     * 按筛选条件导出订单 Excel
     */
    byte[] exportOrdersExcel(OrderQueryDTO queryDTO);

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

    /**
     * 编辑订单（结构化字段）
     */
    void updateOrder(OrderUpdateDTO dto);

    /**
     * 新增还款记录并累加实收金额（不超过订单总额）
     */
    void addRepayment(OrderRepaymentAddDTO dto);

    /**
     * 删除一笔还款记录（逻辑删除），并重算订单累计收款与支付状态
     */
    void removeRepayment(Long repaymentId);

    /**
     * 关联订单附件（如发票图片）
     */
    void addAttachment(OrderAttachmentAddDTO dto);

    /**
     * 删除订单附件（逻辑删除）
     */
    void removeAttachment(Long attachmentId);
}
