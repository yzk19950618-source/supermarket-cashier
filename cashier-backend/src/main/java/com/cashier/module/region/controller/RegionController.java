package com.cashier.module.region.controller;

import com.cashier.common.result.R;
import com.cashier.module.region.service.RegionReadService;
import com.cashier.module.region.support.RegionSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 区域数据：数据库 {@code region} 表优先（导入 region-init.sql 后生效），否则使用内置示例树。
 */
@Tag(name = "区域数据")
@RestController
@RequestMapping("/api/region")
@RequiredArgsConstructor
public class RegionController {

    private final RegionSource regionSource;
    private final RegionReadService regionReadService;

    @Operation(summary = "全部区域（GET）")
    @GetMapping("/all")
    public R<List<?>> allGet() {
        return R.ok(allPayload());
    }

    @Operation(summary = "全部区域（POST）")
    @PostMapping(value = "/all", consumes = MediaType.ALL_VALUE)
    public R<List<?>> allPost() {
        return R.ok(allPayload());
    }

    private List<?> allPayload() {
        if (regionSource.useDatabase()) {
            return regionReadService.cachedDbAllFlat();
        }
        return regionReadService.cachedBuiltinRoots();
    }

    @Operation(summary = "子区域（懒加载）")
    @GetMapping("/children")
    public R<List<Map<String, Object>>> childrenGet(@RequestParam(value = "parentId", required = false) String parentId) {
        return R.ok(childrenPayload(parentId));
    }

    @Operation(summary = "子区域（懒加载，POST）")
    @PostMapping(value = "/children", consumes = MediaType.ALL_VALUE)
    public R<List<Map<String, Object>>> childrenPost(@RequestParam(value = "parentId", required = false) String parentId) {
        return R.ok(childrenPayload(parentId));
    }

    private List<Map<String, Object>> childrenPayload(String parentId) {
        if (regionSource.useDatabase()) {
            return regionReadService.cachedDbChildren(parentId);
        }
        return regionReadService.cachedBuiltinChildren(parentId);
    }
}
