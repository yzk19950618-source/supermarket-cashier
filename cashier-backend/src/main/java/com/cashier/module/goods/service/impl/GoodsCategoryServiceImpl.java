package com.cashier.module.goods.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cashier.module.goods.dto.CategoryDTO;
import com.cashier.module.goods.entity.GoodsCategory;
import com.cashier.module.goods.mapper.GoodsCategoryMapper;
import com.cashier.module.goods.service.GoodsCategoryService;
import com.cashier.module.goods.vo.CategoryVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsCategoryServiceImpl extends ServiceImpl<GoodsCategoryMapper, GoodsCategory>
        implements GoodsCategoryService {

    @Override
    public List<CategoryVO> listAll() {
        List<GoodsCategory> list = lambdaQuery()
                .orderByAsc(GoodsCategory::getSort)
                .orderByAsc(GoodsCategory::getId)
                .list();
        return list.stream()
                .map(cat -> BeanUtil.copyProperties(cat, CategoryVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void addCategory(CategoryDTO dto) {
        GoodsCategory category = new GoodsCategory();
        category.setName(dto.getName());
        category.setParentId(dto.getParentId());
        category.setSort(dto.getSort());
        category.setStatus(dto.getStatus());
        save(category);
    }

    @Override
    public void updateCategory(CategoryDTO dto) {
        GoodsCategory category = new GoodsCategory();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setParentId(dto.getParentId());
        category.setSort(dto.getSort());
        category.setStatus(dto.getStatus());
        updateById(category);
    }
}
