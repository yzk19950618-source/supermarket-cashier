package com.cashier.module.statistics.service;

import com.cashier.module.statistics.vo.*;

import java.util.List;
import java.util.Map;

public interface StatisticsService {

    DashboardVO getDashboardData();

    SalesTrendVO getSalesTrend(Integer days);

    List<SalesRankingVO> getSalesRanking(Integer top);

    List<Map<String, Object>> getCategoryPie();

    List<CashierRankingVO> getCashierRanking(Integer days);
}
