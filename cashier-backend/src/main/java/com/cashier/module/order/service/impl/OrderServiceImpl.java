package com.cashier.module.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cashier.common.constant.CommonConstant;
import com.cashier.common.exception.BusinessException;
import com.cashier.common.result.ResultCode;
import com.cashier.common.utils.OrderNoUtils;
import com.cashier.module.goods.entity.Goods;
import com.cashier.module.goods.mapper.GoodsMapper;
import com.cashier.module.member.entity.Member;
import com.cashier.module.member.mapper.MemberMapper;
import com.cashier.module.order.dto.OrderQueryDTO;
import com.cashier.module.order.dto.SettleDTO;
import com.cashier.module.order.dto.SettleItemDTO;
import com.cashier.module.order.entity.SaleOrder;
import com.cashier.module.order.entity.SaleOrderItem;
import com.cashier.module.order.mapper.SaleOrderItemMapper;
import com.cashier.module.order.mapper.SaleOrderMapper;
import com.cashier.module.order.service.OrderService;
import com.cashier.module.order.vo.OrderDetailVO;
import com.cashier.module.order.vo.OrderVO;
import com.cashier.module.order.vo.TodaySummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 包含收银结算、退款等核心业务逻辑
 *
 * @author cashier
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final GoodsMapper goodsMapper;
    private final MemberMapper memberMapper;

    /**
     * 🔥 收银结算（核心业务方法）
     *
     * 处理流程：
     * 1. 校验商品（存在性、上架状态、库存）
     * 2. 查询会员信息（如有），获取折扣
     * 3. 计算订单金额
     * 4. 生成订单编号
     * 5. 创建订单主表 + 明细表
     * 6. 扣减库存（乐观锁防超卖）
     * 7. 会员余额支付时扣减余额
     * 8. 累加会员积分
     * 9. 返回订单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO settle(SettleDTO dto, Long userId) {
        log.info("开始结算，收银员ID：{}，购物车商品数：{}", userId, dto.getItems().size());

        // ===== 1. 校验商品并计算金额 =====
        List<SaleOrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SettleItemDTO item : dto.getItems()) {
            // 查询商品
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods == null || goods.getDeleted() == 1) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND.getCode(),
                        "商品不存在（ID: " + item.getGoodsId() + "）");
            }
            // 检查上架状态
            if (goods.getStatus() != CommonConstant.STATUS_ENABLED) {
                throw new BusinessException(ResultCode.GOODS_OFF_SHELF.getCode(),
                        "商品「" + goods.getName() + "」已下架");
            }
            // 检查库存
            if (goods.getStock() < item.getQuantity()) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH.getCode(),
                        "商品「" + goods.getName() + "」库存不足，当前库存：" + goods.getStock());
            }

            // 计算小计
            BigDecimal subtotal = goods.getSellingPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(subtotal);

            // 构建明细对象
            SaleOrderItem orderItem = new SaleOrderItem();
            orderItem.setGoodsId(goods.getId());
            orderItem.setGoodsName(goods.getName());
            orderItem.setBarcode(goods.getBarcode());
            orderItem.setSellingPrice(goods.getSellingPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(subtotal);
            orderItems.add(orderItem);
        }

        // ===== 2. 查询会员信息 =====
        Member member = null;
        BigDecimal discount = BigDecimal.ONE;  // 默认无折扣

        if (StrUtil.isNotBlank(dto.getMemberCardNo())) {
            member = memberMapper.selectOne(
                    new LambdaQueryWrapper<Member>()
                            .eq(Member::getCardNo, dto.getMemberCardNo())
                            .eq(Member::getDeleted, 0));
            if (member != null && member.getDiscount() != null) {
                discount = member.getDiscount();
            }
        }

        // ===== 3. 计算金额 =====
        BigDecimal discountAmount = totalAmount.multiply(BigDecimal.ONE.subtract(discount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal realAmount = totalAmount.subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        // ===== 4. 会员余额支付校验 =====
        if (dto.getPayType() == CommonConstant.PAY_TYPE_BALANCE) {
            if (member == null) {
                throw new BusinessException("会员余额支付需要先选择会员");
            }
            if (member.getBalance().compareTo(realAmount) < 0) {
                throw new BusinessException(ResultCode.MEMBER_BALANCE_NOT_ENOUGH);
            }
        }

        // ===== 5. 创建订单主表 =====
        SaleOrder order = new SaleOrder();
        order.setOrderNo(OrderNoUtils.generateOrderNo());
        order.setMemberId(member != null ? member.getId() : null);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setRealAmount(realAmount);
        order.setPayType(dto.getPayType());
        order.setStatus(CommonConstant.ORDER_STATUS_COMPLETED);
        order.setRemark(dto.getRemark());
        saleOrderMapper.insert(order);

        // ===== 6. 创建订单明细 =====
        for (SaleOrderItem item : orderItems) {
            item.setOrderId(order.getId());
            saleOrderItemMapper.insert(item);
        }

        // ===== 7. 扣减库存（乐观锁防超卖） =====
        for (SettleItemDTO item : dto.getItems()) {
            int rows = goodsMapper.deductStock(item.getGoodsId(), item.getQuantity());
            if (rows == 0) {
                // 扣减失败说明库存不足，抛异常回滚事务
                Goods goods = goodsMapper.selectById(item.getGoodsId());
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH.getCode(),
                        "商品「" + (goods != null ? goods.getName() : item.getGoodsId()) + "」库存不足");
            }
        }

        // ===== 8. 会员相关操作 =====
        if (member != null) {
            // 会员余额支付时扣减余额
            if (dto.getPayType() == CommonConstant.PAY_TYPE_BALANCE) {
                int rows = memberMapper.deductBalance(member.getId(), realAmount);
                if (rows == 0) {
                    throw new BusinessException(ResultCode.MEMBER_BALANCE_NOT_ENOUGH);
                }
            }

            // 累加积分（每消费1元积1分）
            int addPoints = realAmount.intValue();
            if (addPoints > 0) {
                memberMapper.addPoints(member.getId(), addPoints);
            }
        }

        log.info("结算成功，订单号：{}，实付金额：{}", order.getOrderNo(), realAmount);

        // ===== 9. 构建返回结果 =====
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(totalAmount);
        vo.setDiscountAmount(discountAmount);
        vo.setRealAmount(realAmount);
        vo.setPayType(dto.getPayType());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        if (member != null) {
            vo.setMemberId(member.getId());
            vo.setMemberName(member.getName());
        }
        return vo;
    }

    @Override
    public IPage<OrderVO> pageList(OrderQueryDTO queryDTO) {
        Page<OrderVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return saleOrderMapper.selectPageVO(page,
                queryDTO.getOrderNo(),
                queryDTO.getPayType(),
                queryDTO.getStatus(),
                queryDTO.getStartDate(),
                queryDTO.getEndDate());
    }

    @Override
    public OrderDetailVO getDetail(Long id) {
        // 查询订单主表
        SaleOrder order = saleOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        OrderDetailVO vo = BeanUtil.copyProperties(order, OrderDetailVO.class);

        // 查询关联信息
        // 收银员名称（简化处理，也可用 XML 关联查询）
        // 会员名称
        if (order.getMemberId() != null) {
            Member member = memberMapper.selectById(order.getMemberId());
            if (member != null) {
                vo.setMemberName(member.getName());
            }
        }

        // 查询订单明细
        List<SaleOrderItem> items = saleOrderItemMapper.selectList(
                new LambdaQueryWrapper<SaleOrderItem>()
                        .eq(SaleOrderItem::getOrderId, id)
                        .orderByAsc(SaleOrderItem::getId));

        List<OrderDetailVO.OrderItemVO> itemVOs = items.stream()
                .map(item -> BeanUtil.copyProperties(item, OrderDetailVO.OrderItemVO.class))
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }

    /**
     * 订单退款
     *
     * 处理流程：
     * 1. 校验订单状态
     * 2. 恢复商品库存
     * 3. 会员余额支付时退回余额
     * 4. 扣减会员积分
     * 5. 修改订单状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id) {
        // 1. 查询并校验订单
        SaleOrder order = saleOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (order.getStatus() == CommonConstant.ORDER_STATUS_REFUNDED) {
            throw new BusinessException(ResultCode.ORDER_ALREADY_REFUNDED);
        }

        // 2. 查询订单明细
        List<SaleOrderItem> items = saleOrderItemMapper.selectList(
                new LambdaQueryWrapper<SaleOrderItem>()
                        .eq(SaleOrderItem::getOrderId, id));

        // 3. 恢复库存
        for (SaleOrderItem item : items) {
            goodsMapper.addStock(item.getGoodsId(), item.getQuantity());
        }

        // 4. 会员相关退款操作
        if (order.getMemberId() != null) {
            // 会员余额支付退回余额
            if (order.getPayType() == CommonConstant.PAY_TYPE_BALANCE) {
                memberMapper.addBalance(order.getMemberId(), order.getRealAmount());
            }

            // 扣减积分
            int deductPoints = order.getRealAmount().intValue();
            if (deductPoints > 0) {
                memberMapper.deductPoints(order.getMemberId(), deductPoints);
            }
        }

        // 5. 修改订单状态为已退款
        SaleOrder updateOrder = new SaleOrder();
        updateOrder.setId(id);
        updateOrder.setStatus(CommonConstant.ORDER_STATUS_REFUNDED);
        saleOrderMapper.updateById(updateOrder);

        log.info("订单退款成功，订单号：{}", order.getOrderNo());
    }

    @Override
    public TodaySummaryVO todaySummary() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // 查询今日已完成订单
        List<SaleOrder> todayOrders = saleOrderMapper.selectList(
                new LambdaQueryWrapper<SaleOrder>()
                        .between(SaleOrder::getCreateTime, todayStart, todayEnd)
                        .eq(SaleOrder::getDeleted, 0));

        TodaySummaryVO summary = new TodaySummaryVO();

        // 已完成订单
        List<SaleOrder> completedOrders = todayOrders.stream()
                .filter(o -> o.getStatus() == CommonConstant.ORDER_STATUS_COMPLETED)
                .collect(Collectors.toList());
        summary.setOrderCount(completedOrders.size());
        summary.setTotalSales(completedOrders.stream()
                .map(SaleOrder::getRealAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 已退款订单
        List<SaleOrder> refundedOrders = todayOrders.stream()
                .filter(o -> o.getStatus() == CommonConstant.ORDER_STATUS_REFUNDED)
                .collect(Collectors.toList());
        summary.setRefundCount(refundedOrders.size());
        summary.setRefundAmount(refundedOrders.stream()
                .map(SaleOrder::getRealAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return summary;
    }
}
