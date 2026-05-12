package com.cashier.common.cache;

import cn.hutool.core.util.StrUtil;
import com.cashier.module.goods.dto.GoodsQueryDTO;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 商品分页查询缓存键（收银台 / 管理端共用同一查询模型）
 */
@Component("goodsQueryCacheKeyGenerator")
public class GoodsQueryCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        GoodsQueryDTO q = (GoodsQueryDTO) params[0];
        StringBuilder sb = new StringBuilder();
        sb.append(q.getPageNum() != null ? q.getPageNum() : 1).append(':');
        sb.append(q.getPageSize() != null ? q.getPageSize() : 10).append(':');
        sb.append(StrUtil.blankToDefault(q.getName(), "")).append(':');
        sb.append(q.getCategoryId() != null ? q.getCategoryId() : "").append(':');
        sb.append(q.getStatus() != null ? q.getStatus() : "");
        return sb.toString();
    }
}
