package com.cashier.module.purchase.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cashier.module.purchase.dto.PurchaseDTO;
import com.cashier.module.purchase.dto.PurchaseQueryDTO;
import com.cashier.module.purchase.entity.PurchaseRecord;
import com.cashier.module.purchase.vo.PurchaseVO;

public interface PurchaseService extends IService<PurchaseRecord> {

    IPage<PurchaseVO> pageList(PurchaseQueryDTO queryDTO);

    void addPurchase(PurchaseDTO dto, Long userId);
}
