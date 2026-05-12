package com.cashier.module.region.service;

import com.cashier.module.region.support.RegionSource;
import com.cashier.module.region.support.RegionTree;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 区域读接口缓存（收银台每次进入仍会请求，后端命中 JVM 缓存减轻数据库压力）
 */
@Service
@RequiredArgsConstructor
public class RegionReadService {

    private final RegionSource regionSource;

    @Cacheable(cacheNames = "regionAllFlat", key = "'dbAll'")
    public List<Map<String, Object>> cachedDbAllFlat() {
        return regionSource.allFlatMaps();
    }

    @Cacheable(cacheNames = "regionChildren", key = "#parentId != null ? #parentId : ''")
    public List<Map<String, Object>> cachedDbChildren(String parentId) {
        return regionSource.childrenMaps(parentId);
    }

    @Cacheable(cacheNames = "regionTreeRoots", key = "'roots'")
    public List<Map<String, Object>> cachedBuiltinRoots() {
        return RegionTree.roots();
    }

    @Cacheable(cacheNames = "regionTreeChildren", key = "#parentId != null ? #parentId : ''")
    public List<Map<String, Object>> cachedBuiltinChildren(String parentId) {
        return RegionTree.childrenOf(parentId);
    }
}
