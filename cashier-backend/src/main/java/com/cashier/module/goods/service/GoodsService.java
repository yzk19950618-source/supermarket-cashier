package com.cashier.module.goods.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cashier.module.goods.dto.GoodsDTO;
import com.cashier.module.goods.dto.GoodsQueryDTO;
import com.cashier.module.goods.dto.GoodsStatusDTO;
import com.cashier.module.goods.entity.Goods;
import com.cashier.module.goods.vo.GoodsImportResultVO;
import com.cashier.module.goods.vo.GoodsVO;
import org.springframework.web.multipart.MultipartFile;

public interface GoodsService extends IService<Goods> {

    /**
     * 分页查询商品
     */
    IPage<GoodsVO> pageList(GoodsQueryDTO queryDTO);

    /**
     * 新增商品
     */
    void addGoods(GoodsDTO dto);

    /**
     * 修改商品
     */
    void updateGoods(GoodsDTO dto);

    /**
     * 修改商品状态
     */
    void updateStatus(GoodsStatusDTO dto);

    /**
     * 查询库存预警商品列表
     */
    IPage<GoodsVO> stockWarningList(GoodsQueryDTO queryDTO);

    /**
     * 按筛选条件导出商品 Excel（与列表筛选一致，最多 5 万条）
     */
    byte[] exportGoodsExcel(GoodsQueryDTO queryDTO);

    /**
     * 批量导入 Excel 模板（仅表头 + 示例说明行）
     */
    byte[] goodsImportTemplate();

    /**
     * 批量导入商品（有商品ID则更新，否则新增）
     */
    GoodsImportResultVO importGoodsBatch(MultipartFile file);
}
