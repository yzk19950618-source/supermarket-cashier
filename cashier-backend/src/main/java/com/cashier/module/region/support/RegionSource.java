package com.cashier.module.region.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cashier.module.region.entity.Region;
import com.cashier.module.region.mapper.RegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 若数据库存在 {@code region} 表且有数据，则区域接口走数据库；否则使用内置 {@link RegionTree}。
 */
@Component
@RequiredArgsConstructor
public class RegionSource {

    private final RegionMapper regionMapper;

    private volatile Boolean databaseMode;

    public boolean useDatabase() {
        if (databaseMode != null) {
            return databaseMode;
        }
        synchronized (this) {
            if (databaseMode != null) {
                return databaseMode;
            }
            try {
                databaseMode = regionMapper.selectCount(new LambdaQueryWrapper<Region>()) > 0;
            } catch (Exception ignored) {
                databaseMode = false;
            }
            return databaseMode;
        }
    }

    /**
     * 全表扁平记录（供前端按 level/parentCode 构建省市区联动）。
     */
    public List<Map<String, Object>> allFlatMaps() {
        List<Region> rows = regionMapper.selectList(
                new LambdaQueryWrapper<Region>().orderByAsc(Region::getSort));
        return rows.stream().map(this::toFlatMap).collect(Collectors.toList());
    }

    /**
     * 直接子节点（parent_code = parentId）；parentId 为空时返回省级（level=1 或 parent 为空）。
     */
    public List<Map<String, Object>> childrenMaps(String parentId) {
        LambdaQueryWrapper<Region> q = new LambdaQueryWrapper<Region>().orderByAsc(Region::getSort);
        if (parentId == null || parentId.isBlank()) {
            q.eq(Region::getLevel, 1);
        } else {
            String pid = parentId.trim();
            q.eq(Region::getParentCode, pid);
        }
        List<Region> rows = regionMapper.selectList(q);
        return rows.stream().map(this::toChildNode).collect(Collectors.toList());
    }

    private Map<String, Object> toFlatMap(Region r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", r.getCode());
        m.put("name", r.getName());
        m.put("parentCode", blankToEmpty(r.getParentCode()));
        m.put("level", r.getLevel());
        m.put("sort", r.getSort());
        return m;
    }

    /** 与 {@link RegionTree} 节点字段保持一致，便于前端复用解析逻辑 */
    private Map<String, Object> toChildNode(Region r) {
        Map<String, Object> m = new LinkedHashMap<>();
        String code = r.getCode();
        String name = r.getName();
        m.put("id", code);
        m.put("name", name);
        m.put("value", code);
        m.put("label", name);
        m.put("code", code);
        m.put("text", name);
        m.put("regionCode", code);
        m.put("regionName", name);
        m.put("children", List.of());
        m.put("isLeaf", Boolean.TRUE);
        return m;
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
