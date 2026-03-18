package com.cashier.module.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cashier.module.goods.dto.CategoryDTO;
import com.cashier.module.goods.entity.GoodsCategory;
import com.cashier.module.goods.vo.CategoryVO;

import java.util.List;

public interface GoodsCategoryService extends IService<GoodsCategory> {

    /**
     * 查询所有分类列表
     */
    List<CategoryVO> listAll();

    /**
     * 新增分类
     */
    void addCategory(CategoryDTO dto);

    /**
     * 修改分类
     */
    void updateCategory(CategoryDTO dto);
}
