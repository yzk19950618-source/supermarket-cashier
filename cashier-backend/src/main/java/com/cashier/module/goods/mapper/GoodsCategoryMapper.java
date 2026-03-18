package com.cashier.module.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cashier.module.goods.entity.GoodsCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类 Mapper 接口
 *
 * @author cashier
 * @since 2024-01-01
 */
@Mapper
public interface GoodsCategoryMapper extends BaseMapper<GoodsCategory> {
}
