package com.cashier.module.supplier.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cashier.module.supplier.dto.SupplierDTO;
import com.cashier.module.supplier.dto.SupplierQueryDTO;
import com.cashier.module.supplier.entity.Supplier;
import com.cashier.module.supplier.vo.SupplierVO;

import java.util.List;

public interface SupplierService extends IService<Supplier> {

    IPage<SupplierVO> pageList(SupplierQueryDTO queryDTO);

    List<SupplierVO> listAll();

    void addSupplier(SupplierDTO dto);

    void updateSupplier(SupplierDTO dto);
}
