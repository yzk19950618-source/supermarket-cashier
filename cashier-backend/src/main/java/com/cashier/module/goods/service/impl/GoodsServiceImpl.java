package com.cashier.module.goods.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cashier.common.exception.BusinessException;
import com.cashier.common.result.ResultCode;
import com.cashier.module.goods.dto.GoodsDTO;
import com.cashier.module.goods.dto.GoodsQueryDTO;
import com.cashier.module.goods.dto.GoodsStatusDTO;
import com.cashier.module.goods.entity.Goods;
import com.cashier.module.goods.mapper.GoodsMapper;
import com.cashier.module.goods.service.GoodsService;
import com.cashier.module.goods.vo.GoodsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商品服务实现类
 *
 * @author cashier
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    @Override
    public IPage<GoodsVO> pageList(GoodsQueryDTO queryDTO) {
        Page<GoodsVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return baseMapper.selectPageVO(page,
                queryDTO.getName(),
                queryDTO.getBarcode(),
                queryDTO.getCategoryId(),
                queryDTO.getStatus());
    }

    @Override
    public GoodsVO getByBarcode(String barcode) {
        Goods goods = lambdaQuery()
                .eq(Goods::getBarcode, barcode)
                .one();
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND.getCode(), "未找到该条码对应的商品");
        }
        return BeanUtil.copyProperties(goods, GoodsVO.class);
    }

    @Override
    public void addGoods(GoodsDTO dto) {
        // 检查条码是否已存在
        Goods existing = lambdaQuery().eq(Goods::getBarcode, dto.getBarcode()).one();
        if (existing != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS.getCode(), "该商品条码已存在");
        }

        Goods goods = BeanUtil.copyProperties(dto, Goods.class);
        goods.setStatus(1);
        save(goods);
    }

    @Override
    public void updateGoods(GoodsDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("商品ID不能为空");
        }

        // 检查条码是否被其他商品占用
        Goods existing = lambdaQuery()
                .eq(Goods::getBarcode, dto.getBarcode())
                .ne(Goods::getId, dto.getId())
                .one();
        if (existing != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS.getCode(), "该商品条码已被其他商品使用");
        }

        Goods goods = BeanUtil.copyProperties(dto, Goods.class);
        updateById(goods);
    }

    @Override
    public void updateStatus(GoodsStatusDTO dto) {
        Goods goods = new Goods();
        goods.setId(dto.getId());
        goods.setStatus(dto.getStatus());
        updateById(goods);
    }

    @Override
    public IPage<GoodsVO> stockWarningList(GoodsQueryDTO queryDTO) {
        Page<GoodsVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        // 查询库存低于预警值的商品
        // 使用自定义 SQL 的方式更方便，此处简化使用 wrapper
        Page<Goods> goodsPage = lambdaQuery()
                .apply("stock <= stock_warning")
                .eq(Goods::getStatus, 1)
                .orderByAsc(Goods::getStock)
                .page(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()));

        return goodsPage.convert(goods -> BeanUtil.copyProperties(goods, GoodsVO.class));
    }
}
