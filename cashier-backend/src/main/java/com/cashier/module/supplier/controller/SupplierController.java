package com.cashier.module.supplier.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cashier.common.dto.IdDTO;
import com.cashier.common.result.R;
import com.cashier.module.supplier.dto.SupplierDTO;
import com.cashier.module.supplier.dto.SupplierQueryDTO;
import com.cashier.module.supplier.service.SupplierService;
import com.cashier.module.supplier.vo.SupplierVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "供应商管理")
@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "分页查询供应商")
    @PostMapping("/page")
    public R<IPage<SupplierVO>> page(@RequestBody SupplierQueryDTO queryDTO) {
        return R.ok(supplierService.pageList(queryDTO));
    }

    @Operation(summary = "查询全部供应商（下拉框用）")
    @PostMapping("/list")
    public R<List<SupplierVO>> list() {
        return R.ok(supplierService.listAll());
    }

    @Operation(summary = "新增供应商")
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid SupplierDTO dto) {
        supplierService.addSupplier(dto);
        return R.ok();
    }

    @Operation(summary = "修改供应商")
    @PostMapping("/update")
    public R<Void> update(@RequestBody @Valid SupplierDTO dto) {
        supplierService.updateSupplier(dto);
        return R.ok();
    }

    @Operation(summary = "删除供应商")
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody @Valid IdDTO dto) {
        supplierService.removeById(dto.getId());
        return R.ok();
    }
}
