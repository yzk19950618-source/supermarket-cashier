package com.cashier.module.order.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cashier.common.dto.IdDTO;
import com.cashier.common.result.R;
import com.cashier.module.order.dto.OrderQueryDTO;
import com.cashier.module.order.dto.SettleDTO;
import com.cashier.module.order.service.OrderService;
import com.cashier.module.order.vo.OrderDetailVO;
import com.cashier.module.order.vo.OrderVO;
import com.cashier.module.order.vo.TodaySummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单管理控制器
 *
 * @author cashier
 * @since 2024-01-01
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 收银结算（核心接口）
     */
    @Operation(summary = "结算下单")
    @PostMapping("/settle")
    public R<OrderVO> settle(@RequestBody @Valid SettleDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(orderService.settle(dto, userId));
    }

    @Operation(summary = "分页查询订单")
    @PostMapping("/page")
    public R<IPage<OrderVO>> page(@RequestBody OrderQueryDTO queryDTO) {
        return R.ok(orderService.pageList(queryDTO));
    }

    @Operation(summary = "查询订单详情")
    @PostMapping("/detail")
    public R<OrderDetailVO> detail(@RequestBody @Valid IdDTO dto) {
        return R.ok(orderService.getDetail(dto.getId()));
    }

    @Operation(summary = "订单退款")
    @PostMapping("/refund")
    public R<Void> refund(@RequestBody @Valid IdDTO dto) {
        orderService.refund(dto.getId());
        return R.ok();
    }

    @Operation(summary = "今日订单汇总")
    @PostMapping("/today")
    public R<TodaySummaryVO> today() {
        return R.ok(orderService.todaySummary());
    }
}
