package com.cashier.module.goods.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cashier.common.dto.IdDTO;
import com.cashier.common.result.R;
import com.cashier.module.goods.dto.GoodsDTO;
import com.cashier.module.goods.dto.GoodsQueryDTO;
import com.cashier.module.goods.dto.GoodsStatusDTO;
import com.cashier.module.goods.service.GoodsService;
import com.cashier.module.goods.vo.GoodsImportResultVO;
import com.cashier.module.goods.vo.GoodsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

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

    @Operation(summary = "按筛选条件导出商品 Excel")
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody GoodsQueryDTO queryDTO) {
        byte[] body = goodsService.exportGoodsExcel(queryDTO);
        String filename = "商品导出_" + LocalDate.now() + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @Operation(summary = "下载商品批量导入模板")
    @PostMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] body = goodsService.goodsImportTemplate();
        String filename = "商品导入模板_" + LocalDate.now() + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @Operation(summary = "批量导入商品（Excel）")
    @PostMapping(value = "/import/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<GoodsImportResultVO> importBatch(@RequestPart("file") MultipartFile file) {
        return R.ok(goodsService.importGoodsBatch(file));
    }
}
