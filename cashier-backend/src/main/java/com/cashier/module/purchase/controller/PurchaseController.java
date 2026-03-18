package com.cashier.module.purchase.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cashier.common.dto.IdDTO;
import com.cashier.common.result.R;
import com.cashier.module.purchase.dto.PurchaseDTO;
import com.cashier.module.purchase.dto.PurchaseQueryDTO;
import com.cashier.module.purchase.service.PurchaseService;
import com.cashier.module.purchase.vo.PurchaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "进货管理")
@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Operation(summary = "分页查询进货记录")
    @PostMapping("/page")
    public R<IPage<PurchaseVO>> page(@RequestBody PurchaseQueryDTO queryDTO) {
        return R.ok(purchaseService.pageList(queryDTO));
    }

    @Operation(summary = "新增进货记录")
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid PurchaseDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        purchaseService.addPurchase(dto, userId);
        return R.ok();
    }

    @Operation(summary = "删除进货记录")
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody @Valid IdDTO dto) {
        purchaseService.removeById(dto.getId());
        return R.ok();
    }
}
