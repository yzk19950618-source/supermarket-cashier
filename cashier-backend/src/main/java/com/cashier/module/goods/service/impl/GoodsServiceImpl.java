package com.cashier.module.goods.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cashier.common.exception.BusinessException;
import com.cashier.module.goods.dto.GoodsDTO;
import com.cashier.module.goods.dto.GoodsQueryDTO;
import com.cashier.module.goods.dto.GoodsStatusDTO;
import com.cashier.module.goods.entity.Goods;
import com.cashier.module.goods.mapper.GoodsMapper;
import com.cashier.module.goods.service.GoodsService;
import com.cashier.module.goods.vo.GoodsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.Serializable;

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
    @Cacheable(cacheNames = "goodsPageVo", keyGenerator = "goodsQueryCacheKeyGenerator", unless = "#result == null")
    public IPage<GoodsVO> pageList(GoodsQueryDTO queryDTO) {
        Page<GoodsVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return baseMapper.selectPageVO(page,
                queryDTO.getName(),
                queryDTO.getCategoryId(),
                queryDTO.getStatus());
    }

    @Override
    @CacheEvict(cacheNames = {"goodsPageVo", "goodsStockWarning"}, allEntries = true)
    public void addGoods(GoodsDTO dto) {
        Goods goods = BeanUtil.copyProperties(dto, Goods.class);
        goods.setStatus(1);
        goods.setBarcode(nextUniqueBarcode());
        try {
            save(goods);
        } catch (DuplicateKeyException e) {
            // uk_barcode 冲突（极低概率），换号重试一次
            goods.setBarcode(nextUniqueBarcode());
            save(goods);
        }
    }

    @Override
    @CacheEvict(cacheNames = {"goodsPageVo", "goodsStockWarning"}, allEntries = true)
    public void updateGoods(GoodsDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("商品ID不能为空");
        }

        Goods existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        Goods goods = BeanUtil.copyProperties(dto, Goods.class);
        goods.setBarcode(StrUtil.isBlank(existing.getBarcode()) ? nextUniqueBarcode() : existing.getBarcode());
        updateById(goods);
    }

    /** 生成主表条码（≤50，满足离线 init 的 VARCHAR(50) + uk_barcode） */
    private static String nextUniqueBarcode() {
        return "G" + IdUtil.fastSimpleUUID();
    }

    @Override
    @CacheEvict(cacheNames = {"goodsPageVo", "goodsStockWarning"}, allEntries = true)
    public void updateStatus(GoodsStatusDTO dto) {
        Goods goods = new Goods();
        goods.setId(dto.getId());
        goods.setStatus(dto.getStatus());
        updateById(goods);
    }

    @Override
    @Cacheable(cacheNames = "goodsStockWarning", keyGenerator = "goodsQueryCacheKeyGenerator", unless = "#result == null")
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

    @Override
    @CacheEvict(cacheNames = {"goodsPageVo", "goodsStockWarning"}, allEntries = true)
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }
}
