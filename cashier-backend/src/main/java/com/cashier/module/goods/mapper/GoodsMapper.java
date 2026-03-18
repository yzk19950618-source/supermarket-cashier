package com.cashier.module.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cashier.module.goods.entity.Goods;
import com.cashier.module.goods.vo.GoodsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 Mapper 接口
 *
 * @author cashier
 * @since 2024-01-01
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 分页查询商品列表（关联分类名称）
     *
     * @param page       分页参数
     * @param name       商品名称
     * @param barcode    商品条码
     * @param categoryId 分类ID
     * @param status     状态
     * @return 分页结果
     */
    IPage<GoodsVO> selectPageVO(Page<GoodsVO> page,
                                @Param("name") String name,
                                @Param("barcode") String barcode,
                                @Param("categoryId") Long categoryId,
                                @Param("status") Integer status);

    /**
     * 扣减库存（乐观锁）
     *
     * @param goodsId  商品ID
     * @param quantity 扣减数量
     * @return 影响行数（0表示库存不足）
     */
    @Update("UPDATE goods SET stock = stock - #{quantity} WHERE id = #{goodsId} AND stock >= #{quantity} AND deleted = 0")
    int deductStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity);

    /**
     * 增加库存
     *
     * @param goodsId  商品ID
     * @param quantity 增加数量
     */
    @Update("UPDATE goods SET stock = stock + #{quantity} WHERE id = #{goodsId} AND deleted = 0")
    int addStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity);
}
