package com.cashier.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cashier.common.constant.CommonConstant;
import com.cashier.module.order.entity.SaleOrder;
import com.cashier.module.order.entity.SaleOrderItem;
import com.cashier.module.order.mapper.SaleOrderItemMapper;
import com.cashier.module.order.mapper.SaleOrderMapper;
import com.cashier.module.user.entity.User;
import com.cashier.module.user.mapper.UserMapper;
import com.cashier.module.goods.entity.GoodsCategory;
import com.cashier.module.goods.mapper.GoodsCategoryMapper;
import com.cashier.module.goods.entity.Goods;
import com.cashier.module.goods.mapper.GoodsMapper;
import com.cashier.module.statistics.service.StatisticsService;
import com.cashier.module.statistics.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计服务实现类
 * 提供看板数据、销售趋势、排行等统计分析功能
 *
 * @author cashier
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final UserMapper userMapper;
    private final GoodsCategoryMapper categoryMapper;
    private final GoodsMapper goodsMapper;

    @Override
    public DashboardVO getDashboardData() {
        DashboardVO vo = new DashboardVO();

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(today, LocalTime.MAX);

        // 今日已完成订单
        List<SaleOrder> todayOrders = saleOrderMapper.selectList(
                new LambdaQueryWrapper<SaleOrder>()
                        .between(SaleOrder::getCreateTime, todayStart, todayEnd)
                        .eq(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_COMPLETED)
                        .eq(SaleOrder::getDeleted, 0));

        // 今日销售额
        BigDecimal todaySales = todayOrders.stream()
                .map(SaleOrder::getRealAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTodaySales(todaySales);

        // 今日订单数
        vo.setTodayOrders(todayOrders.size());

        // 今日客单价
        if (!todayOrders.isEmpty()) {
            vo.setTodayAvgPrice(todaySales.divide(
                    BigDecimal.valueOf(todayOrders.size()), 2, RoundingMode.HALF_UP));
        } else {
            vo.setTodayAvgPrice(BigDecimal.ZERO);
        }

        // 本月销售额
        LocalDateTime monthStart = LocalDateTime.of(today.withDayOfMonth(1), LocalTime.MIN);
        List<SaleOrder> monthOrders = saleOrderMapper.selectList(
                new LambdaQueryWrapper<SaleOrder>()
                        .ge(SaleOrder::getCreateTime, monthStart)
                        .le(SaleOrder::getCreateTime, todayEnd)
                        .eq(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_COMPLETED)
                        .eq(SaleOrder::getDeleted, 0));
        BigDecimal monthSales = monthOrders.stream()
                .map(SaleOrder::getRealAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthSales(monthSales);

        return vo;
    }

    @Override
    public SalesTrendVO getSalesTrend(Integer days) {
        SalesTrendVO vo = new SalesTrendVO();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(formatter));

            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 查询当天已完成订单的销售额
            List<SaleOrder> dayOrders = saleOrderMapper.selectList(
                    new LambdaQueryWrapper<SaleOrder>()
                            .between(SaleOrder::getCreateTime, dayStart, dayEnd)
                            .eq(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_COMPLETED)
                            .eq(SaleOrder::getDeleted, 0));

            BigDecimal daySales = dayOrders.stream()
                    .map(SaleOrder::getRealAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amounts.add(daySales);
        }

        vo.setDates(dates);
        vo.setAmounts(amounts);
        return vo;
    }

    @Override
    public List<SalesRankingVO> getSalesRanking(Integer top) {
        // 查询所有已完成订单的明细
        // 实际项目中建议用 SQL 聚合查询，此处简化实现
        LocalDateTime startDate = LocalDateTime.of(LocalDate.now().minusDays(30), LocalTime.MIN);

        // 获取近30天完成的订单ID
        List<SaleOrder> orders = saleOrderMapper.selectList(
                new LambdaQueryWrapper<SaleOrder>()
                        .ge(SaleOrder::getCreateTime, startDate)
                        .eq(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_COMPLETED)
                        .eq(SaleOrder::getDeleted, 0)
                        .select(SaleOrder::getId));

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orderIds = orders.stream().map(SaleOrder::getId).collect(Collectors.toList());

        // 查询订单明细
        List<SaleOrderItem> items = saleOrderItemMapper.selectList(
                new LambdaQueryWrapper<SaleOrderItem>()
                        .in(SaleOrderItem::getOrderId, orderIds));

        // 按商品聚合
        Map<String, SalesRankingVO> rankingMap = new LinkedHashMap<>();
        for (SaleOrderItem item : items) {
            SalesRankingVO ranking = rankingMap.computeIfAbsent(item.getGoodsName(), name -> {
                SalesRankingVO r = new SalesRankingVO();
                r.setGoodsName(name);
                r.setTotalQuantity(0);
                r.setTotalAmount(BigDecimal.ZERO);
                return r;
            });
            ranking.setTotalQuantity(ranking.getTotalQuantity() + item.getQuantity());
            ranking.setTotalAmount(ranking.getTotalAmount().add(item.getSubtotal()));
        }

        // 按销量排序取 TOP N
        return rankingMap.values().stream()
                .sorted((a, b) -> b.getTotalQuantity().compareTo(a.getTotalQuantity()))
                .limit(top)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getCategoryPie() {
        // 查询近30天已完成订单的明细，按分类聚合销售额
        LocalDateTime startDate = LocalDateTime.of(LocalDate.now().minusDays(30), LocalTime.MIN);

        List<SaleOrder> orders = saleOrderMapper.selectList(
                new LambdaQueryWrapper<SaleOrder>()
                        .ge(SaleOrder::getCreateTime, startDate)
                        .eq(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_COMPLETED)
                        .eq(SaleOrder::getDeleted, 0)
                        .select(SaleOrder::getId));

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orderIds = orders.stream().map(SaleOrder::getId).collect(Collectors.toList());
        List<SaleOrderItem> items = saleOrderItemMapper.selectList(
                new LambdaQueryWrapper<SaleOrderItem>()
                        .in(SaleOrderItem::getOrderId, orderIds));

        // 获取所有分类
        Map<Long, String> categoryMap = new HashMap<>();
        List<GoodsCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<GoodsCategory>().eq(GoodsCategory::getDeleted, 0));
        categories.forEach(c -> categoryMap.put(c.getId(), c.getName()));

        // 获取商品对应的分类
        Map<Long, Long> goodsCategoryMap = new HashMap<>();
        List<Goods> goodsList = goodsMapper.selectList(
                new LambdaQueryWrapper<Goods>().eq(Goods::getDeleted, 0).select(Goods::getId, Goods::getCategoryId));
        goodsList.forEach(g -> goodsCategoryMap.put(g.getId(), g.getCategoryId()));

        // 按分类聚合
        Map<String, BigDecimal> categoryAmountMap = new LinkedHashMap<>();
        for (SaleOrderItem item : items) {
            Long categoryId = goodsCategoryMap.get(item.getGoodsId());
            String categoryName = categoryMap.getOrDefault(categoryId, "未分类");
            categoryAmountMap.merge(categoryName, item.getSubtotal(), BigDecimal::add);
        }

        // 转为 ECharts 饼图格式
        List<Map<String, Object>> result = new ArrayList<>();
        categoryAmountMap.forEach((name, value) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("value", value);
            result.add(map);
        });

        return result;
    }

    @Override
    public List<CashierRankingVO> getCashierRanking(Integer days) {
        LocalDateTime startDate = LocalDateTime.of(LocalDate.now().minusDays(days), LocalTime.MIN);

        // 查询指定时间段的已完成订单
        List<SaleOrder> orders = saleOrderMapper.selectList(
                new LambdaQueryWrapper<SaleOrder>()
                        .ge(SaleOrder::getCreateTime, startDate)
                        .eq(SaleOrder::getStatus, CommonConstant.ORDER_STATUS_COMPLETED)
                        .eq(SaleOrder::getDeleted, 0));

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有用户
        Map<Long, String> userMap = new HashMap<>();
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0));
        users.forEach(u -> userMap.put(u.getId(), u.getRealName()));

        // 按收银员聚合
        Map<Long, CashierRankingVO> rankingMap = new LinkedHashMap<>();
        for (SaleOrder order : orders) {
            CashierRankingVO ranking = rankingMap.computeIfAbsent(order.getUserId(), id -> {
                CashierRankingVO r = new CashierRankingVO();
                r.setUserName(userMap.getOrDefault(id, "未知"));
                r.setOrderCount(0);
                r.setTotalAmount(BigDecimal.ZERO);
                return r;
            });
            ranking.setOrderCount(ranking.getOrderCount() + 1);
            ranking.setTotalAmount(ranking.getTotalAmount().add(order.getRealAmount()));
        }

        // 计算客单价并排序
        return rankingMap.values().stream()
                .peek(r -> r.setAvgPrice(r.getOrderCount() > 0
                        ? r.getTotalAmount().divide(BigDecimal.valueOf(r.getOrderCount()), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO))
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }
}
