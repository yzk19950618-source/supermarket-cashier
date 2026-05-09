package com.cashier.module.region.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置省市区树与懒加载子节点索引（示例数据）。
 */
public final class RegionTree {

    private static final List<Map<String, Object>> ROOTS = new ArrayList<>();
    private static final Map<String, List<Map<String, Object>>> CHILDREN_BY_PARENT = new HashMap<>();

    static {
        List<Map<String, Object>> tree = buildSampleTree();
        ROOTS.addAll(tree);
        for (Map<String, Object> n : tree) {
            registerChildrenIndex(n);
        }
    }

    private RegionTree() {
    }

    public static List<Map<String, Object>> roots() {
        return ROOTS;
    }

    /**
     * @param parentId 父级 region code；null 或空表示省级根
     */
    public static List<Map<String, Object>> childrenOf(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return ROOTS;
        }
        return CHILDREN_BY_PARENT.getOrDefault(parentId, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private static void registerChildrenIndex(Map<String, Object> node) {
        List<Map<String, Object>> ch = (List<Map<String, Object>>) node.get("children");
        if (ch == null || ch.isEmpty()) {
            return;
        }
        CHILDREN_BY_PARENT.put(String.valueOf(node.get("value")), ch);
        for (Map<String, Object> c : ch) {
            registerChildrenIndex(c);
        }
    }

    private static List<Map<String, Object>> buildSampleTree() {
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(province("110000", "北京市",
                city("110100", "市辖区", district("110101", "东城区"), district("110102", "西城区"))));
        out.add(province("310000", "上海市",
                city("310100", "市辖区", district("310101", "黄浦区"), district("310104", "徐汇区"))));
        out.add(province("440000", "广东省",
                city("440100", "广州市", district("440103", "荔湾区"), district("440104", "越秀区"))));
        return out;
    }

    private static Map<String, Object> province(String code, String name, Map<String, Object>... children) {
        return node(code, name, List.of(children));
    }

    private static Map<String, Object> city(String code, String name, Map<String, Object>... children) {
        return node(code, name, List.of(children));
    }

    private static Map<String, Object> district(String code, String name) {
        return node(code, name, null);
    }

    private static Map<String, Object> node(String code, String name, List<Map<String, Object>> children) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", code);
        m.put("name", name);
        m.put("value", code);
        m.put("label", name);
        m.put("code", code);
        m.put("text", name);
        m.put("regionCode", code);
        m.put("regionName", name);
        if (children != null && !children.isEmpty()) {
            m.put("children", children);
            m.put("isLeaf", Boolean.FALSE);
        } else {
            m.put("children", Collections.emptyList());
            m.put("isLeaf", Boolean.TRUE);
        }
        return m;
    }
}
