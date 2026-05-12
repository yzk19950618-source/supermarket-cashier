package com.cashier.module.goods.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cashier.common.exception.BusinessException;
import com.cashier.module.goods.dto.GoodsDTO;
import com.cashier.module.goods.dto.GoodsQueryDTO;
import com.cashier.module.goods.dto.GoodsStatusDTO;
import com.cashier.module.goods.entity.Goods;
import com.cashier.module.goods.entity.GoodsCategory;
import com.cashier.module.goods.mapper.GoodsCategoryMapper;
import com.cashier.module.goods.mapper.GoodsMapper;
import com.cashier.module.goods.service.GoodsService;
import com.cashier.module.goods.vo.GoodsImportResultVO;
import com.cashier.module.goods.vo.GoodsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 商品服务实现类
 *
 * @author cashier
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    private final GoodsCategoryMapper goodsCategoryMapper;

    private static final int GOODS_EXPORT_MAX = 50000;
    private static final int GOODS_IMPORT_MAX_ROWS = 5000;

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
        if (goods.getPromoEnabled() == null) {
            goods.setPromoEnabled(0);
        }
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

    @Override
    public byte[] exportGoodsExcel(GoodsQueryDTO queryDTO) {
        List<GoodsVO> list = baseMapper.selectExportList(
                StrUtil.trimToNull(queryDTO.getName()),
                queryDTO.getCategoryId(),
                queryDTO.getStatus(),
                GOODS_EXPORT_MAX);
        List<List<Object>> data = new ArrayList<>();
        data.add(List.of(
                "商品ID", "条码", "商品名称", "分类名称", "单位", "进货价", "零售价", "库存", "预警值",
                "状态(0下架/1上架)", "同款买满送(0关/1开)", "买满件数", "赠送数量", "图片URL"));
        for (GoodsVO g : list) {
            data.add(List.of(
                    g.getId(),
                    g.getBarcode(),
                    g.getName(),
                    g.getCategoryName(),
                    g.getUnit(),
                    dec(g.getPurchasePrice()),
                    dec(g.getSellingPrice()),
                    g.getStock() != null ? String.valueOf(g.getStock()) : "",
                    g.getStockWarning() != null ? String.valueOf(g.getStockWarning()) : "",
                    g.getStatus(),
                    g.getPromoEnabled() != null ? g.getPromoEnabled() : 0,
                    g.getPromoBuyQty() != null ? g.getPromoBuyQty() : "",
                    g.getPromoGiftQty() != null ? g.getPromoGiftQty().stripTrailingZeros().toPlainString() : "",
                    StrUtil.nullToEmpty(g.getImage())));
        }
        var writer = ExcelUtil.getWriter(true);
        writer.write(data, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.flush(out);
        writer.close();
        return out.toByteArray();
    }

    @Override
    public byte[] goodsImportTemplate() {
        List<List<Object>> data = new ArrayList<>();
        data.add(List.of(
                "商品ID", "商品名称", "分类名称", "单位", "进货价", "零售价", "库存", "预警值",
                "同款买满送(0关/1开)", "买满件数", "赠送数量", "图片URL"));
        data.add(List.of(
                "",
                "（示例）新商品名称",
                "（请改为已存在的分类名称）",
                "件",
                "1.00",
                "2.00",
                "0",
                "10",
                "0",
                "",
                "",
                ""));
        var writer = ExcelUtil.getWriter(true);
        writer.write(data, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.flush(out);
        writer.close();
        return out.toByteArray();
    }

    @Override
    @CacheEvict(cacheNames = {"goodsPageVo", "goodsStockWarning"}, allEntries = true)
    public GoodsImportResultVO importGoodsBatch(MultipartFile file) {
        GoodsImportResultVO res = new GoodsImportResultVO();
        if (file == null || file.isEmpty()) {
            res.getErrors().add("请上传 Excel 文件");
            return res;
        }
        List<Map<String, Object>> rows;
        try {
            var reader = ExcelUtil.getReader(file.getInputStream(), 0);
            rows = reader.readAll();
        } catch (Exception e) {
            res.getErrors().add("无法读取 Excel：" + e.getMessage());
            return res;
        }
        if (rows == null) {
            rows = List.of();
        }
        if (rows.size() > GOODS_IMPORT_MAX_ROWS) {
            res.getErrors().add("单次最多导入 " + GOODS_IMPORT_MAX_ROWS + " 行，请拆分文件");
            return res;
        }
        res.setTotal(rows.size());
        int ok = 0;
        int fail = 0;
        int lineBase = 2;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            int excelRow = lineBase + i;
            try {
                String name = cellStr(getCell(row, "商品名称", "名称"));
                if (StrUtil.isBlank(name) || name.startsWith("（示例）") || name.contains("请改为")) {
                    continue;
                }
                String catName = cellStr(getCell(row, "分类名称", "分类"));
                if (StrUtil.isBlank(catName)) {
                    throw new BusinessException("分类名称不能为空");
                }
                if (catName.contains("请改为")) {
                    continue;
                }
                GoodsCategory cat = goodsCategoryMapper.selectOne(
                        new LambdaQueryWrapper<GoodsCategory>()
                                .eq(GoodsCategory::getDeleted, 0)
                                .eq(GoodsCategory::getName, catName.trim())
                                .last("LIMIT 1"));
                if (cat == null) {
                    throw new BusinessException("未找到分类「" + catName.trim() + "」");
                }
                GoodsDTO dto = new GoodsDTO();
                dto.setName(name.trim());
                dto.setCategoryId(cat.getId());
                dto.setUnit(StrUtil.blankToDefault(cellStr(getCell(row, "单位")), "件"));
                dto.setPurchasePrice(requireBd(row, "进货价"));
                dto.setSellingPrice(requireBd(row, "零售价"));
                BigDecimal st = optBd(getCell(row, "库存"));
                dto.setStock(st == null ? 0 : st.setScale(0, RoundingMode.HALF_UP).intValue());
                BigDecimal sw = optBd(getCell(row, "预警值", "预警"));
                dto.setStockWarning(sw == null ? 10 : sw.setScale(0, RoundingMode.HALF_UP).intValue());
                dto.setImage(StrUtil.trimToNull(cellStr(getCell(row, "图片URL", "图片"))));
                Integer pe = optInt(getCell(row, "同款买满送(0关/1开)", "同款买满送"));
                dto.setPromoEnabled(pe != null ? pe : 0);
                dto.setPromoBuyQty(optInt(getCell(row, "买满件数")));
                dto.setPromoGiftQty(optBd(getCell(row, "赠送数量")));

                Long id = optLong(getCell(row, "商品ID", "ID"));
                if (id != null) {
                    dto.setId(id);
                    updateGoods(dto);
                } else {
                    addGoods(dto);
                }
                ok++;
            } catch (Exception e) {
                fail++;
                if (res.getErrors().size() < 80) {
                    res.getErrors().add("第" + excelRow + "行：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                }
            }
        }
        res.setSuccess(ok);
        res.setFail(fail);
        return res;
    }

    private static Object getCell(Map<String, Object> row, String... aliases) {
        for (Map.Entry<String, Object> e : row.entrySet()) {
            String hk = e.getKey() == null ? "" : e.getKey().replaceAll("\\s+", "");
            if (hk.isEmpty()) {
                continue;
            }
            for (String a : aliases) {
                String an = a.replaceAll("\\s+", "");
                if (hk.equals(an) || hk.contains(an)) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    private static String cellStr(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        if (o instanceof Number n) {
            return String.valueOf(n);
        }
        return String.valueOf(o).trim();
    }

    private static BigDecimal optBd(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        return new BigDecimal(s);
    }

    private static BigDecimal requireBd(Map<String, Object> row, String header) {
        Object v = getCell(row, header);
        BigDecimal b = optBd(v);
        if (b == null) {
            throw new BusinessException(header + "不能为空");
        }
        return b.setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer optInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        return Integer.parseInt(s);
    }

    private static Long optLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        return Long.parseLong(s);
    }

    private static String dec(BigDecimal v) {
        if (v == null) {
            return "";
        }
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
