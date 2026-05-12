package com.cashier.module.purchase.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cashier.common.exception.BusinessException;
import com.cashier.common.result.ResultCode;
import com.cashier.common.utils.OrderNoUtils;
import com.cashier.module.goods.entity.Goods;
import com.cashier.module.goods.mapper.GoodsMapper;
import com.cashier.module.purchase.dto.PurchaseDTO;
import com.cashier.module.purchase.dto.PurchaseQueryDTO;
import com.cashier.module.purchase.entity.PurchaseRecord;
import com.cashier.module.purchase.mapper.PurchaseRecordMapper;
import com.cashier.module.purchase.service.PurchaseService;
import com.cashier.module.purchase.vo.PurchaseVO;
import com.cashier.module.supplier.entity.Supplier;
import com.cashier.module.supplier.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 进货服务实现类
 * 进货时自动增加对应商品库存
 *
 * @author cashier
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl extends ServiceImpl<PurchaseRecordMapper, PurchaseRecord>
        implements PurchaseService {

    private final GoodsMapper goodsMapper;
    private final SupplierMapper supplierMapper;

    @Override
    public IPage<PurchaseVO> pageList(PurchaseQueryDTO queryDTO) {
        LambdaQueryWrapper<PurchaseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getPurchaseNo()), PurchaseRecord::getPurchaseNo, queryDTO.getPurchaseNo())
                .eq(queryDTO.getSupplierId() != null, PurchaseRecord::getSupplierId, queryDTO.getSupplierId())
                .orderByDesc(PurchaseRecord::getCreateTime);

        Page<PurchaseRecord> page = page(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);

        return page.convert(record -> {
            PurchaseVO vo = BeanUtil.copyProperties(record, PurchaseVO.class);
            // 关联查询供应商名称
            Supplier supplier = supplierMapper.selectById(record.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getName());
            }
            // 关联查询商品名称
            Goods goods = goodsMapper.selectById(record.getGoodsId());
            if (goods != null) {
                vo.setGoodsName(goods.getName());
            }
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPurchase(PurchaseDTO dto, Long userId) {
        // 校验商品是否存在
        Goods goods = goodsMapper.selectById(dto.getGoodsId());
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND.getCode(), "商品不存在");
        }

        // 计算总金额
        BigDecimal totalAmount = dto.getPurchasePrice()
                .multiply(BigDecimal.valueOf(dto.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        // 创建进货记录
        PurchaseRecord record = new PurchaseRecord();
        record.setPurchaseNo(OrderNoUtils.generatePurchaseNo());
        record.setSupplierId(dto.getSupplierId());
        record.setGoodsId(dto.getGoodsId());
        record.setQuantity(dto.getQuantity());
        record.setPurchasePrice(dto.getPurchasePrice());
        record.setTotalAmount(totalAmount);
        record.setUserId(userId);
        record.setRemark(dto.getRemark());
        save(record);

        // 增加商品库存
        goodsMapper.addStock(dto.getGoodsId(), BigDecimal.valueOf(dto.getQuantity()));

        // 更新商品进货价（以最新进货价为准）
        Goods updateGoods = new Goods();
        updateGoods.setId(dto.getGoodsId());
        updateGoods.setPurchasePrice(dto.getPurchasePrice());
        goodsMapper.updateById(updateGoods);
    }
}
