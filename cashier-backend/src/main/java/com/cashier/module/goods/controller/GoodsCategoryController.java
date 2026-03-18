package com.cashier.module.goods.controller;

import com.cashier.common.dto.IdDTO;
import com.cashier.common.result.R;
import com.cashier.module.goods.dto.CategoryDTO;
import com.cashier.module.goods.service.GoodsCategoryService;
import com.cashier.module.goods.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品分类管理")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class GoodsCategoryController {

    private final GoodsCategoryService categoryService;

    @Operation(summary = "查询分类列表")
    @PostMapping("/list")
    public R<List<CategoryVO>> list() {
        return R.ok(categoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid CategoryDTO dto) {
        categoryService.addCategory(dto);
        return R.ok();
    }

    @Operation(summary = "修改分类")
    @PostMapping("/update")
    public R<Void> update(@RequestBody @Valid CategoryDTO dto) {
        categoryService.updateCategory(dto);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody @Valid IdDTO dto) {
        categoryService.removeById(dto.getId());
        return R.ok();
    }
}
