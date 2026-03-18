package com.cashier.module.supplier.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cashier.module.supplier.dto.SupplierDTO;
import com.cashier.module.supplier.dto.SupplierQueryDTO;
import com.cashier.module.supplier.entity.Supplier;
import com.cashier.module.supplier.mapper.SupplierMapper;
import com.cashier.module.supplier.service.SupplierService;
import com.cashier.module.supplier.vo.SupplierVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    @Override
    public IPage<SupplierVO> pageList(SupplierQueryDTO queryDTO) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getName()), Supplier::getName, queryDTO.getName())
                .like(StrUtil.isNotBlank(queryDTO.getContact()), Supplier::getContact, queryDTO.getContact())
                .orderByDesc(Supplier::getCreateTime);

        Page<Supplier> page = page(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        return page.convert(s -> BeanUtil.copyProperties(s, SupplierVO.class));
    }

    @Override
    public List<SupplierVO> listAll() {
        List<Supplier> list = lambdaQuery()
                .eq(Supplier::getStatus, 1)
                .orderByDesc(Supplier::getCreateTime)
                .list();
        return list.stream()
                .map(s -> BeanUtil.copyProperties(s, SupplierVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void addSupplier(SupplierDTO dto) {
        Supplier supplier = new Supplier();
        supplier.setName(dto.getName());
        supplier.setContact(dto.getContact());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setStatus(1);
        save(supplier);
    }

    @Override
    public void updateSupplier(SupplierDTO dto) {
        Supplier supplier = new Supplier();
        supplier.setId(dto.getId());
        supplier.setName(dto.getName());
        supplier.setContact(dto.getContact());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        updateById(supplier);
    }
}
