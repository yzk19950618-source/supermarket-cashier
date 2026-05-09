package com.cashier.module.region.controller;

import com.cashier.common.result.R;
import com.cashier.module.region.support.RegionTree;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 省市区树（示例数据）；支持 GET/POST 与懒加载子节点。
 */
@Tag(name = "区域数据")
@RestController
@RequestMapping("/api/region")
public class RegionController {

    @Operation(summary = "全部区域（GET）")
    @GetMapping("/all")
    public R<List<Map<String, Object>>> allGet() {
        return R.ok(RegionTree.roots());
    }

    @Operation(summary = "全部区域（POST）")
    @PostMapping(value = "/all", consumes = MediaType.ALL_VALUE)
    public R<List<Map<String, Object>>> allPost() {
        return R.ok(RegionTree.roots());
    }

    @Operation(summary = "子区域（懒加载）")
    @GetMapping("/children")
    public R<List<Map<String, Object>>> childrenGet(@RequestParam(value = "parentId", required = false) String parentId) {
        return R.ok(RegionTree.childrenOf(parentId));
    }

    @Operation(summary = "子区域（懒加载，POST）")
    @PostMapping(value = "/children", consumes = MediaType.ALL_VALUE)
    public R<List<Map<String, Object>>> childrenPost(@RequestParam(value = "parentId", required = false) String parentId) {
        return R.ok(RegionTree.childrenOf(parentId));
    }
}
