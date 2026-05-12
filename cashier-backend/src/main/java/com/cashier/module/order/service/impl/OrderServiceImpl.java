package com.cashier.module.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cashier.common.constant.CommonConstant;
import com.cashier.common.exception.BusinessException;
import com.cashier.common.result.ResultCode;
import com.cashier.common.utils.OrderNoUtils;
import com.cashier.common.utils.SyntheticMemberPhone;
import com.cashier.module.goods.entity.Goods;
import com.cashier.module.goods.entity.GoodsCategory;
import com.cashier.module.goods.mapper.GoodsCategoryMapper;
import com.cashier.module.goods.mapper.GoodsMapper;
import com.cashier.module.member.entity.Member;
import com.cashier.module.member.mapper.MemberMapper;
import com.cashier.module.order.dto.OrderAttachmentAddDTO;
import com.cashier.module.order.dto.OrderQueryDTO;
import com.cashier.module.order.dto.OrderRepaymentAddDTO;
import com.cashier.module.order.dto.OrderUpdateDTO;
import com.cashier.module.order.dto.SettleDTO;
import com.cashier.module.order.dto.SettleItemDTO;
import com.cashier.module.order.entity.SaleOrderAttachment;
import com.cashier.module.order.entity.SaleOrderRepayment;
import com.cashier.module.order.entity.SaleOrder;
import com.cashier.module.order.entity.SaleOrderItem;
import com.cashier.module.order.mapper.SaleOrderAttachmentMapper;
import com.cashier.module.order.mapper.SaleOrderItemMapper;
import com.cashier.module.order.mapper.SaleOrderMapper;
import com.cashier.module.order.mapper.SaleOrderRepaymentMapper;
import com.cashier.module.order.service.OrderService;
import com.cashier.module.order.vo.OrderDetailVO;
import com.cashier.module.order.vo.OrderRepaymentVO;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final MemberMapper memberMapper;
    private final SaleOrderRepaymentMapper saleOrderRepaymentMapper;
    private final SaleOrderAttachmentMapper saleOrderAttachmentMapper;

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

        if (dto.getPayType() == null) {
            dto.setPayType(CommonConstant.PAY_TYPE_CASH);
        }

        // ===== 1. 校验商品并计算金额 =====
        List<SaleOrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, String> categoryNameCache = new HashMap<>();

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
            orderItem.setCategoryName(resolveCategoryName(goods.getCategoryId(), categoryNameCache));
            orderItem.setSellingPrice(goods.getSellingPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(subtotal);
            orderItems.add(orderItem);
        }

        String prefName = StrUtil.blankToDefault(dto.getCustomerName(), "").trim();
        if (StrUtil.isBlank(prefName)) {
            prefName = extractRemarkSegment(dto.getRemark(), "客户");
        }
        String prefPhone = StrUtil.blankToDefault(dto.getCustomerPhone(), "").trim();
        if (StrUtil.isBlank(prefPhone)) {
            prefPhone = extractRemarkSegment(dto.getRemark(), "电话");
        }
        String prefAddr = StrUtil.blankToDefault(dto.getCustomerAddress(), "").trim();
        if (StrUtil.isBlank(prefAddr)) {
            prefAddr = extractRemarkSegment(dto.getRemark(), "收货地址");
        }
        Integer custGender = dto.getCustomerGender() != null ? dto.getCustomerGender() : 0;

        // ===== 2. 会员（卡号/手机；有客户姓名时可自动建档并关联订单）=====
        Member member = resolveMemberForSettle(dto, prefName, prefPhone, prefAddr, custGender);
        BigDecimal discount = BigDecimal.ONE;
        if (member != null && member.getDiscount() != null) {
            discount = member.getDiscount();
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
        order.setPayType(dto.getPayType());
        order.setRemark(truncateVarchar255(mergeFreeRemarks(dto)));
        /** 赊销默认：未支付、累计实收 0；仅会员余额全额支付视为当场结清 */
        boolean balanceInstantPay = dto.getPayType() != null
                && dto.getPayType() == CommonConstant.PAY_TYPE_BALANCE;
        if (balanceInstantPay) {
            order.setRealAmount(realAmount);
            order.setStatus(CommonConstant.ORDER_STATUS_PAID);
            order.setPaidTime(LocalDateTime.now());
        } else {
            order.setRealAmount(BigDecimal.ZERO);
            order.setStatus(CommonConstant.ORDER_STATUS_UNPAID);
            order.setPaidTime(null);
        }
        String customerName = prefName;
        if (StrUtil.isBlank(customerName) && member != null && StrUtil.isNotBlank(member.getName())) {
            customerName = member.getName().trim();
        }
        if (StrUtil.isBlank(customerName)) {
            customerName = extractRemarkSegment(dto.getRemark(), "客户");
        }
        order.setCustomerName(StrUtil.blankToDefault(customerName, ""));
        String customerPhone = prefPhone;
        if (member != null && SyntheticMemberPhone.isSynthetic(member.getPhone())) {
            customerPhone = "";
        } else if (StrUtil.isBlank(customerPhone) && member != null && StrUtil.isNotBlank(member.getPhone())) {
            customerPhone = member.getPhone().trim();
        }
        if (StrUtil.isBlank(customerPhone)) {
            customerPhone = extractRemarkSegment(dto.getRemark(), "电话");
        }
        order.setCustomerPhone(StrUtil.blankToDefault(customerPhone, ""));
        LocalDate repay = resolveOrderDate(dto.getRepayDate(), dto.getRemark(), "还款日期");
        order.setRepayDate(repay != null ? repay : LocalDate.now());
        LocalDate delivery = resolveOrderDate(dto.getDeliveryDate(), dto.getRemark(), "送货日期");
        order.setDeliveryDate(delivery != null ? delivery : LocalDate.now());
        String addr = prefAddr;
        if (StrUtil.isBlank(addr)) {
            addr = extractRemarkSegment(dto.getRemark(), "收货地址");
        }
        order.setCustomerAddress(StrUtil.isBlank(addr) ? null : addr);
        order.setCustomerGender(custGender);
        LocalDate orderDate = parseLocalDateOrNull(dto.getOrderDate());
        order.setOrderDate(orderDate != null ? orderDate : LocalDate.now());
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

        // ===== 8. 会员相关操作（仅余额当场结清时扣款、积分） =====
        if (member != null && balanceInstantPay) {
            int rows = memberMapper.deductBalance(member.getId(), realAmount);
            if (rows == 0) {
                throw new BusinessException(ResultCode.MEMBER_BALANCE_NOT_ENOUGH);
            }
            int addPoints = realAmount.intValue();
            if (addPoints > 0) {
                memberMapper.addPoints(member.getId(), addPoints);
            }
        }

        syncMemberProfileFromSettle(member, order);

        log.info("结算成功，订单号：{}，应收(优惠后)：{}，状态：{}", order.getOrderNo(), realAmount, order.getStatus());

        // ===== 9. 构建返回结果 =====
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(totalAmount);
        vo.setDiscountAmount(discountAmount);
        vo.setRealAmount(order.getRealAmount());
        vo.setPayType(dto.getPayType());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setRemark(order.getRemark());
        vo.setCustomerName(order.getCustomerName());
        vo.setCustomerPhone(order.getCustomerPhone());
        vo.setRepayDate(order.getRepayDate());
        vo.setDeliveryDate(order.getDeliveryDate());
        vo.setCustomerAddress(order.getCustomerAddress());
        vo.setCustomerGender(order.getCustomerGender());
        vo.setOrderDate(order.getOrderDate());
        vo.setPaidTime(order.getPaidTime());
        if (member != null) {
            vo.setMemberId(member.getId());
            vo.setMemberName(member.getName());
        }
        fillDebtSummary(vo, order.getStatus());
        return vo;
    }

    @Override
    public IPage<OrderVO> pageList(OrderQueryDTO queryDTO) {
        Page<OrderVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<OrderVO> out = saleOrderMapper.selectPageVO(page,
                queryDTO.getOrderNo(),
                queryDTO.getPayType(),
                queryDTO.getStatus(),
                queryDTO.getStartDate(),
                queryDTO.getEndDate(),
                queryDTO.getMemberId());
        for (OrderVO row : out.getRecords()) {
            fillDebtSummary(row, row.getStatus());
        }
        return out;
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

        List<SaleOrderRepayment> rpRows = saleOrderRepaymentMapper.selectList(
                new LambdaQueryWrapper<SaleOrderRepayment>()
                        .eq(SaleOrderRepayment::getOrderId, id)
                        .eq(SaleOrderRepayment::getDeleted, 0)
                        .orderByDesc(SaleOrderRepayment::getCreateTime));
        List<OrderRepaymentVO> repaymentVOs = new ArrayList<>();
        for (SaleOrderRepayment r : rpRows) {
            OrderRepaymentVO rv = new OrderRepaymentVO();
            rv.setId(r.getId());
            rv.setAmount(r.getAmount());
            rv.setPayType(r.getPayType());
            rv.setRemark(r.getRemark());
            rv.setCreateTime(r.getCreateTime());
            rv.setOperatorName("-");
            repaymentVOs.add(rv);
        }
        vo.setRepayments(repaymentVOs);

        List<SaleOrderAttachment> attRows = saleOrderAttachmentMapper.selectList(
                new LambdaQueryWrapper<SaleOrderAttachment>()
                        .eq(SaleOrderAttachment::getOrderId, id)
                        .eq(SaleOrderAttachment::getDeleted, 0)
                        .orderByAsc(SaleOrderAttachment::getId));
        List<OrderDetailVO.OrderAttachmentVO> attachmentVOs = attRows.stream().map(a -> {
            OrderDetailVO.OrderAttachmentVO av = new OrderDetailVO.OrderAttachmentVO();
            av.setId(a.getId());
            av.setAttachmentType(a.getAttachmentType());
            av.setUrl(a.getUrl());
            return av;
        }).collect(Collectors.toList());
        vo.setAttachments(attachmentVOs);
        vo.setInvoiceUrls(attRows.stream()
                .filter(a -> a.getAttachmentType() != null
                        && a.getAttachmentType() == SaleOrderAttachment.TYPE_INVOICE)
                .map(SaleOrderAttachment::getUrl)
                .collect(Collectors.toList()));

        BigDecimal paidForSummary = repaymentPaidSum(repaymentVOs, order.getRealAmount());
        fillDebtSummary(vo, order.getStatus(), paidForSummary);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(OrderUpdateDTO dto) {
        SaleOrder existing = saleOrderMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (existing.getStatus() != null && existing.getStatus() == CommonConstant.ORDER_STATUS_REFUNDED) {
            throw new BusinessException("已退款订单不可编辑金额");
        }
        if (dto.getRemainDebt() != null) {
            applyRemainDebtAndDiscount(existing, dto.getRemainDebt());
        }
        boolean metaUpdate = false;
        LambdaUpdateWrapper<SaleOrder> uw = new LambdaUpdateWrapper<SaleOrder>()
                .eq(SaleOrder::getId, dto.getId());
        if (dto.getRemainDebt() == null && dto.getRealAmount() != null) {
            uw.set(SaleOrder::getRealAmount, dto.getRealAmount());
            metaUpdate = true;
        }
        if (dto.getRemark() != null) {
            uw.set(SaleOrder::getRemark, truncateVarchar255(dto.getRemark()));
            metaUpdate = true;
        }
        if (dto.getCustomerName() != null) {
            uw.set(SaleOrder::getCustomerName, dto.getCustomerName());
            metaUpdate = true;
        }
        if (dto.getCustomerPhone() != null) {
            uw.set(SaleOrder::getCustomerPhone, dto.getCustomerPhone());
            metaUpdate = true;
        }
        if (dto.getCustomerAddress() != null) {
            uw.set(SaleOrder::getCustomerAddress, dto.getCustomerAddress());
            metaUpdate = true;
        }
        LocalDate rd = parseLocalDateOrNull(dto.getRepayDate());
        if (rd != null) {
            uw.set(SaleOrder::getRepayDate, rd);
            metaUpdate = true;
        }
        LocalDate dd = parseLocalDateOrNull(dto.getDeliveryDate());
        if (dd != null) {
            uw.set(SaleOrder::getDeliveryDate, dd);
            metaUpdate = true;
        }
        LocalDate od = parseLocalDateOrNull(dto.getOrderDate());
        if (od != null) {
            uw.set(SaleOrder::getOrderDate, od);
            metaUpdate = true;
        }
        if (metaUpdate) {
            saleOrderMapper.update(null, uw);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRepayment(OrderRepaymentAddDTO dto) {
        if (dto.getPayType() == CommonConstant.PAY_TYPE_BALANCE) {
            throw new BusinessException("还款记录不支持「会员余额」支付方式");
        }
        SaleOrder order = saleOrderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        BigDecimal add = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (add.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("还款金额须大于 0");
        }

        SaleOrderRepayment row = new SaleOrderRepayment();
        row.setOrderId(dto.getOrderId());
        row.setAmount(add);
        row.setPayType(dto.getPayType());
        row.setRemark(truncateVarchar255(StrUtil.blankToDefault(dto.getRemark(), null)));
        saleOrderRepaymentMapper.insert(row);

        BigDecimal cur = order.getRealAmount() != null ? order.getRealAmount() : BigDecimal.ZERO;
        BigDecimal due = orderPayableDue(order);
        BigDecimal next = cur.add(add);
        if (next.compareTo(due) > 0) {
            next = due;
        }
        LambdaUpdateWrapper<SaleOrder> uw = new LambdaUpdateWrapper<SaleOrder>()
                .eq(SaleOrder::getId, dto.getOrderId())
                .set(SaleOrder::getRealAmount, next);
        if (next.compareTo(due) >= 0 && order.getStatus() != CommonConstant.ORDER_STATUS_REFUNDED) {
            uw.set(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_PAID);
            uw.set(SaleOrder::getPaidTime, LocalDateTime.now());
        }
        saleOrderMapper.update(null, uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRepayment(Long repaymentId) {
        SaleOrderRepayment row = saleOrderRepaymentMapper.selectById(repaymentId);
        if (row == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        SaleOrder order = saleOrderMapper.selectById(row.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (order.getStatus() != null && order.getStatus() == CommonConstant.ORDER_STATUS_REFUNDED) {
            throw new BusinessException("已退款订单不可删除还款记录");
        }
        saleOrderRepaymentMapper.deleteById(repaymentId);
        refreshOrderCollectedFromRepayments(row.getOrderId());
    }

    /** 根据未删除的还款明细汇总订单累计收款 {@code real_amount}，并同步支付状态 */
    private void refreshOrderCollectedFromRepayments(Long orderId) {
        SaleOrder order = saleOrderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        if (order.getStatus() != null && order.getStatus() == CommonConstant.ORDER_STATUS_REFUNDED) {
            return;
        }
        List<SaleOrderRepayment> rows = saleOrderRepaymentMapper.selectList(
                new LambdaQueryWrapper<SaleOrderRepayment>()
                        .eq(SaleOrderRepayment::getOrderId, orderId)
                        .eq(SaleOrderRepayment::getDeleted, 0));
        BigDecimal sum = rows.stream()
                .map(SaleOrderRepayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal due = orderPayableDue(order);
        BigDecimal next = sum.compareTo(due) > 0 ? due : sum;

        LambdaUpdateWrapper<SaleOrder> uw = new LambdaUpdateWrapper<SaleOrder>()
                .eq(SaleOrder::getId, orderId)
                .set(SaleOrder::getRealAmount, next);
        if (next.compareTo(due) >= 0) {
            uw.set(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_PAID);
            if (order.getPaidTime() == null) {
                uw.set(SaleOrder::getPaidTime, LocalDateTime.now());
            }
        } else {
            uw.set(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_UNPAID);
            uw.set(SaleOrder::getPaidTime, null);
        }
        saleOrderMapper.update(null, uw);
    }

    @Override
    public void addAttachment(OrderAttachmentAddDTO dto) {
        SaleOrder order = saleOrderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        int type = dto.getAttachmentType() != null ? dto.getAttachmentType() : SaleOrderAttachment.TYPE_INVOICE;
        if (type != SaleOrderAttachment.TYPE_INVOICE
                && type != SaleOrderAttachment.TYPE_IOU
                && type != SaleOrderAttachment.TYPE_DELIVERY_IMAGE) {
            throw new BusinessException("附件类型须为 1-发票、2-欠条、3-送货图片");
        }
        SaleOrderAttachment a = new SaleOrderAttachment();
        a.setOrderId(dto.getOrderId());
        a.setAttachmentType(type);
        a.setUrl(dto.getUrl().trim());
        saleOrderAttachmentMapper.insert(a);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAttachment(Long attachmentId) {
        SaleOrderAttachment row = saleOrderAttachmentMapper.selectById(attachmentId);
        if (row == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        saleOrderAttachmentMapper.deleteById(attachmentId);
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
                .filter(o -> o.getStatus() == CommonConstant.ORDER_STATUS_PAID)
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

    private String resolveCategoryName(Long categoryId, Map<Long, String> cache) {
        if (categoryId == null) {
            return "-";
        }
        return cache.computeIfAbsent(categoryId, id -> {
            GoodsCategory c = goodsCategoryMapper.selectById(id);
            return c != null && StrUtil.isNotBlank(c.getName()) ? c.getName() : "-";
        });
    }

    private String mergeFreeRemarks(SettleDTO dto) {
        List<String> lines = new ArrayList<>();
        if (StrUtil.isNotBlank(dto.getCustomerRemark())) {
            lines.add(dto.getCustomerRemark().trim());
        }
        if (StrUtil.isNotBlank(dto.getOrderRemark())) {
            lines.add(dto.getOrderRemark().trim());
        }
        if (StrUtil.isNotBlank(dto.getPricingNote())) {
            lines.add("定价说明：" + dto.getPricingNote().trim());
        }
        if (StrUtil.isNotBlank(dto.getRegionCodes())) {
            lines.add("区划编码：" + dto.getRegionCodes().trim());
        }
        if (StrUtil.isNotBlank(dto.getRemark())) {
            lines.add(dto.getRemark().trim());
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    /** 与离线库表 remark VARCHAR(255) 一致 */
    private static String truncateVarchar255(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() <= 255) {
            return s;
        }
        return s.substring(0, 255);
    }

    /**
     * 结算时已关联会员：将订单上的姓名/电话/性别/地址写回会员档案（与订单一致），变更写入备注；手机号与他人冲突则跳过改号。
     */
    private void syncMemberProfileFromSettle(Member member, SaleOrder order) {
        if (member == null || member.getId() == null) {
            return;
        }
        Member db = memberMapper.selectById(member.getId());
        if (db == null) {
            return;
        }
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<String> lines = new ArrayList<>();
        LambdaUpdateWrapper<Member> uw = new LambdaUpdateWrapper<Member>().eq(Member::getId, member.getId());
        boolean any = false;

        String newName = StrUtil.trim(order.getCustomerName());
        String oldName = StrUtil.nullToDefault(db.getName(), "").trim();
        if (StrUtil.isNotBlank(newName) && !newName.equals(oldName)) {
            uw.set(Member::getName, truncateMemberVarchar50(newName));
            lines.add(timeStr + " 姓名修改：" + snippet(oldName, 40) + " → " + snippet(newName, 40));
            any = true;
        }

        String newPhone = StrUtil.trim(order.getCustomerPhone());
        String oldPhone = StrUtil.nullToDefault(db.getPhone(), "").trim();
        if (StrUtil.isNotBlank(newPhone)
                && !SyntheticMemberPhone.isSynthetic(newPhone)
                && !newPhone.equals(oldPhone)) {
            Long conflict = memberMapper.selectCount(
                    new LambdaQueryWrapper<Member>()
                            .eq(Member::getPhone, newPhone)
                            .ne(Member::getId, member.getId())
                            .eq(Member::getDeleted, 0));
            if (conflict == null || conflict == 0L) {
                uw.set(Member::getPhone, truncateMemberVarchar20(newPhone));
                lines.add(timeStr + " 电话修改：" + snippet(oldPhone, 24) + " → " + snippet(newPhone, 24));
                any = true;
            } else {
                log.warn("结算同步会员电话跳过：{} 已被其他客户占用", newPhone);
            }
        }

        Integer newGender = order.getCustomerGender();
        if (newGender != null && !Objects.equals(newGender, db.getGender())) {
            uw.set(Member::getGender, newGender);
            lines.add(timeStr + " 性别修改：" + genderLabel(db.getGender()) + " → " + genderLabel(newGender));
            any = true;
        }

        String newAddr = StrUtil.trim(order.getCustomerAddress());
        String oldAddr = StrUtil.nullToDefault(db.getAddress(), "").trim();
        if (StrUtil.isNotBlank(newAddr) && !newAddr.equals(oldAddr)) {
            uw.set(Member::getAddress, truncateVarchar255(newAddr));
            lines.add(timeStr + " 地址修改：" + snippet(oldAddr, 60) + " → " + snippet(newAddr, 60));
            any = true;
        }

        if (!any) {
            return;
        }
        String mergedRemark = db.getRemark();
        for (String line : lines) {
            mergedRemark = appendMemberRemarkLine(mergedRemark, line);
        }
        uw.set(Member::getRemark, mergedRemark);
        memberMapper.update(null, uw);
    }

    private static String snippet(String s, int max) {
        String t = StrUtil.nullToDefault(s, "").trim();
        if (StrUtil.isBlank(t)) {
            return "（空）";
        }
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private static String genderLabel(Integer g) {
        if (g == null) {
            return "未知";
        }
        return switch (g) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }

    private static String truncateMemberVarchar50(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 50 ? s : s.substring(0, 50);
    }

    private static String truncateMemberVarchar20(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 20 ? s : s.substring(0, 20);
    }

    /** 会员表 remark 长度见 {@link CommonConstant#MEMBER_REMARK_MAX_LENGTH}，新记录追加在末尾 */
    private static String appendMemberRemarkLine(String prev, String line) {
        String p = StrUtil.blankToDefault(prev, "").trim();
        String l = StrUtil.blankToDefault(line, "").trim();
        if (StrUtil.isBlank(l)) {
            return truncateMemberRemark(StrUtil.isBlank(p) ? null : p);
        }
        String next = StrUtil.isBlank(p) ? l : p + "\n" + l;
        return truncateMemberRemark(next);
    }

    private static String truncateMemberRemark(String s) {
        if (s == null) {
            return null;
        }
        int max = CommonConstant.MEMBER_REMARK_MAX_LENGTH;
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private LocalDate resolveOrderDate(String dtoDate, String remark, String remarkLabel) {
        LocalDate fromDto = parseLocalDateOrNull(dtoDate);
        if (fromDto != null) {
            return fromDto;
        }
        return parseLocalDateOrNull(extractRemarkSegment(remark, remarkLabel));
    }

    private static LocalDate parseLocalDateOrNull(String s) {
        if (StrUtil.isBlank(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 与收银备注分段一致：「标签:值」，如 客户、电话 */
    private static String extractRemarkSegment(String remark, String label) {
        if (StrUtil.isBlank(remark) || StrUtil.isBlank(label)) {
            return "";
        }
        for (String piece : remark.split("[;；]\\s*")) {
            String p = piece.trim();
            int idx = Math.max(p.indexOf(':'), p.indexOf('：'));
            if (idx <= 0 || idx >= p.length() - 1) {
                continue;
            }
            String k = p.substring(0, idx).trim();
            String v = p.substring(idx + 1).trim();
            if (label.equals(k)) {
                return v;
            }
        }
        return "";
    }

    /** 订单应收（优惠后）= 商品总额 − 优惠金额；还款累计到此金额即视为结清 */
    private static BigDecimal orderPayableDue(SaleOrder order) {
        return payableDueAmount(order.getTotalAmount(), order.getDiscountAmount());
    }

    private static BigDecimal payableDueAmount(BigDecimal totalAmount, BigDecimal discountAmount) {
        BigDecimal total = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BigDecimal disc = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal due = total.subtract(disc);
        if (due.compareTo(BigDecimal.ZERO) < 0) {
            due = BigDecimal.ZERO;
        }
        return due.setScale(2, RoundingMode.HALF_UP);
    }

    /** 解析结算请求中的会员（已选客户 ID / 卡号/手机/建档） */
    private Member resolveMemberForSettle(SettleDTO dto,
            String prefName,
            String prefPhone,
            String prefAddr,
            Integer custGender) {
        if (dto.getMemberId() != null) {
            Member byId = memberMapper.selectOne(
                    new LambdaQueryWrapper<Member>()
                            .eq(Member::getId, dto.getMemberId())
                            .eq(Member::getDeleted, 0));
            if (byId != null) {
                return byId;
            }
        }
        if (StrUtil.isNotBlank(dto.getMemberCardNo())) {
            Member byCard = memberMapper.selectOne(
                    new LambdaQueryWrapper<Member>()
                            .eq(Member::getPhone, dto.getMemberCardNo().trim())
                            .eq(Member::getDeleted, 0));
            if (byCard != null) {
                return byCard;
            }
        }

        String lookupPhone = StrUtil.trimToEmpty(prefPhone);
        if (SyntheticMemberPhone.isSynthetic(lookupPhone)) {
            lookupPhone = "";
        }
        if (StrUtil.isNotBlank(lookupPhone)) {
            Member byPhone = memberMapper.selectOne(
                    new LambdaQueryWrapper<Member>()
                            .eq(Member::getPhone, lookupPhone)
                            .eq(Member::getDeleted, 0));
            if (byPhone != null) {
                return byPhone;
            }
        }

        if (StrUtil.isBlank(prefName)) {
            return null;
        }
        return findOrCreateGuestMember(prefName,
                StrUtil.isBlank(lookupPhone) ? null : lookupPhone,
                StrUtil.trimToNull(prefAddr),
                custGender);
    }

    private Member findOrCreateGuestMember(String name,
            String realPhoneNullable,
            String address,
            Integer gender) {
        if (realPhoneNullable != null) {
            Member ex = memberMapper.selectOne(
                    new LambdaQueryWrapper<Member>()
                            .eq(Member::getPhone, realPhoneNullable.trim())
                            .eq(Member::getDeleted, 0));
            if (ex != null) {
                return ex;
            }
        }

        Member m = new Member();
        m.setName(name.trim());
        String phoneStored = realPhoneNullable != null ? realPhoneNullable.trim() : "";
        if (StrUtil.isBlank(phoneStored)) {
            phoneStored = null;
            for (int i = 0; i < 12; i++) {
                String p = SyntheticMemberPhone.next();
                Long cnt = memberMapper.selectCount(
                        new LambdaQueryWrapper<Member>()
                                .eq(Member::getPhone, p)
                                .eq(Member::getDeleted, 0));
                if (cnt == null || cnt == 0L) {
                    phoneStored = p;
                    break;
                }
            }
            if (StrUtil.isBlank(phoneStored)) {
                throw new BusinessException("创建客户失败：无法生成占位手机号");
            }
        }
        m.setPhone(phoneStored);
        m.setGender(gender != null ? gender : 0);
        m.setAddress(address);
        m.setRemark(null);
        m.setBalance(BigDecimal.ZERO);
        m.setPoints(0);
        m.setDiscount(BigDecimal.ONE);
        m.setStatus(1);
        memberMapper.insert(m);
        return m;
    }

    /** 还款展示用「已还」：有明细则求和，否则回落到库里的累计已收 */
    private static BigDecimal repaymentPaidSum(List<OrderRepaymentVO> repayments, BigDecimal fallbackReal) {
        if (repayments == null || repayments.isEmpty()) {
            return fallbackReal != null ? fallbackReal.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return repayments.stream()
                .map(OrderRepaymentVO::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 手动修改目标剩余欠款：还款合计不变，按「总额 − 已还 − 目标欠款」反算优惠金额，并同步 {@code real_amount}、支付状态。
     */
    private void applyRemainDebtAndDiscount(SaleOrder order, BigDecimal targetRemainDebt) {
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        List<SaleOrderRepayment> rows = saleOrderRepaymentMapper.selectList(
                new LambdaQueryWrapper<SaleOrderRepayment>()
                        .eq(SaleOrderRepayment::getOrderId, order.getId())
                        .eq(SaleOrderRepayment::getDeleted, 0));
        BigDecimal paidSum = rows.stream()
                .map(SaleOrderRepayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (rows.isEmpty()) {
            paidSum = order.getRealAmount() != null ? order.getRealAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal debt = targetRemainDebt.setScale(2, RoundingMode.HALF_UP);
        if (debt.compareTo(BigDecimal.ZERO) < 0) {
            debt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal maxDebt = total.subtract(paidSum).setScale(2, RoundingMode.HALF_UP);
        if (maxDebt.compareTo(BigDecimal.ZERO) < 0) {
            maxDebt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (debt.compareTo(maxDebt) > 0) {
            debt = maxDebt;
        }

        BigDecimal discountNew = total.subtract(paidSum).subtract(debt).setScale(2, RoundingMode.HALF_UP);
        if (discountNew.compareTo(BigDecimal.ZERO) < 0) {
            discountNew = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal maxDiscount = total.setScale(2, RoundingMode.HALF_UP);
        if (discountNew.compareTo(maxDiscount) > 0) {
            discountNew = maxDiscount;
        }

        BigDecimal receivable = payableDueAmount(total, discountNew);
        BigDecimal realNext = paidSum.min(receivable);

        LambdaUpdateWrapper<SaleOrder> uw = new LambdaUpdateWrapper<SaleOrder>()
                .eq(SaleOrder::getId, order.getId())
                .set(SaleOrder::getDiscountAmount, discountNew)
                .set(SaleOrder::getRealAmount, realNext);

        BigDecimal remain = receivable.subtract(realNext).setScale(2, RoundingMode.HALF_UP);
        if (remain.compareTo(BigDecimal.ZERO) <= 0) {
            uw.set(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_PAID);
            if (order.getPaidTime() == null) {
                uw.set(SaleOrder::getPaidTime, LocalDateTime.now());
            }
        } else {
            uw.set(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_UNPAID);
            uw.set(SaleOrder::getPaidTime, null);
        }
        saleOrderMapper.update(null, uw);
    }

    /**
     * 填充应收合计 / 已还 / 剩余欠款，供列表、详情、结算返回及老前端直接展示（勿再用 totalAmount−realAmount 当欠款）。
     */
    private static void fillDebtSummary(OrderVO vo, Integer orderStatus) {
        fillDebtSummary(vo, orderStatus, null);
    }

    private static void fillDebtSummary(OrderVO vo, Integer orderStatus, BigDecimal paidOverride) {
        if (vo == null) {
            return;
        }
        BigDecimal receivable = payableDueAmount(vo.getTotalAmount(), vo.getDiscountAmount());
        BigDecimal paid = paidOverride != null
                ? paidOverride.setScale(2, RoundingMode.HALF_UP)
                : (vo.getRealAmount() != null ? vo.getRealAmount() : BigDecimal.ZERO);
        paid = paid.setScale(2, RoundingMode.HALF_UP);
        if (paidOverride != null) {
            vo.setRealAmount(paid);
        }
        BigDecimal remain = receivable.subtract(paid).setScale(2, RoundingMode.HALF_UP);
        if (remain.compareTo(BigDecimal.ZERO) < 0) {
            remain = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (orderStatus != null && orderStatus == CommonConstant.ORDER_STATUS_REFUNDED) {
            remain = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        vo.setReceivableAmount(receivable);
        vo.setPaidAmount(paid);
        vo.setRemainDebt(remain);
    }
}
