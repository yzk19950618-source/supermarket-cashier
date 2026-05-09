package com.cashier.module.statistics.controller;

import com.cashier.common.result.R;
import com.cashier.module.statistics.dto.RankingQueryDTO;
import com.cashier.module.statistics.dto.TrendQueryDTO;
import com.cashier.module.statistics.service.StatisticsService;
import com.cashier.module.statistics.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 数据统计控制器
 *
 * @author cashier
 * @since 2024-01-01
 */
@Tag(name = "数据统计")
@RestController
@RequestMapping(value = "/api/statistics", consumes = MediaType.ALL_VALUE)
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "首页看板数据")
    @PostMapping("/dashboard")
    public R<DashboardVO> dashboard() {
        return R.ok(statisticsService.getDashboardData());
    }

    /**
     * 首页扩展：回款/还款提醒列表（暂无业务数据时返回空列表）。
     */
    @Operation(summary = "回款提醒列表")
    @PostMapping("/repaymentReminder")
    public R<List<Map<String, Object>>> repaymentReminder() {
        return R.ok(Collections.emptyList());
    }

    @Operation(summary = "销售趋势")
    @PostMapping("/salesTrend")
    public R<SalesTrendVO> salesTrend(@RequestBody TrendQueryDTO dto) {
        return R.ok(statisticsService.getSalesTrend(dto.getDays()));
    }

    @Operation(summary = "商品销量排行")
    @PostMapping("/salesRanking")
    public R<List<SalesRankingVO>> salesRanking(@RequestBody RankingQueryDTO dto) {
        return R.ok(statisticsService.getSalesRanking(dto.getTop()));
    }

    @Operation(summary = "分类销售占比")
    @PostMapping("/categoryPie")
    public R<List<Map<String, Object>>> categoryPie() {
        return R.ok(statisticsService.getCategoryPie());
    }

    @Operation(summary = "收银员业绩排行")
    @PostMapping("/cashierRanking")
    public R<List<CashierRankingVO>> cashierRanking(@RequestBody RankingQueryDTO dto) {
        return R.ok(statisticsService.getCashierRanking(dto.getDays()));
    }
}
