package com.cashier.module.goods.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cashier.common.dto.IdDTO;
import com.cashier.common.result.R;
import com.cashier.module.goods.dto.*;
import com.cashier.module.goods.service.GoodsService;
import com.cashier.module.goods.vo.GoodsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品管理控制器
 *
 * @author cashier
 * @since 2024-01-01
 */
@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "分页查询商品列表")
    @PostMapping("/page")
    public R<IPage<GoodsVO>> page(@RequestBody GoodsQueryDTO queryDTO) {
        return R.ok(goodsService.pageList(queryDTO));
    }

    @Operation(summary = "根据条码查询商品")
    @PostMapping("/getByBarcode")
    public R<GoodsVO> getByBarcode(@RequestBody @Valid BarcodeDTO dto) {
        return R.ok(goodsService.getByBarcode(dto.getBarcode()));
    }

    @Operation(summary = "新增商品")
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid GoodsDTO dto) {
        goodsService.addGoods(dto);
        return R.ok();
    }

    @Operation(summary = "修改商品")
    @PostMapping("/update")
    public R<Void> update(@RequestBody @Valid GoodsDTO dto) {
        goodsService.updateGoods(dto);
        return R.ok();
    }

    @Operation(summary = "删除商品")
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody @Valid IdDTO dto) {
        goodsService.removeById(dto.getId());
        return R.ok();
    }

    @Operation(summary = "修改商品状态（上架/下架）")
    @PostMapping("/updateStatus")
    public R<Void> updateStatus(@RequestBody @Valid GoodsStatusDTO dto) {
        goodsService.updateStatus(dto);
        return R.ok();
    }

    @Operation(summary = "库存预警列表")
    @PostMapping("/stockWarning")
    public R<IPage<GoodsVO>> stockWarning(@RequestBody GoodsQueryDTO queryDTO) {
        return R.ok(goodsService.stockWarningList(queryDTO));
    }
}
