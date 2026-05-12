package com.cashier.common.utils;

import cn.hutool.core.util.RandomUtil;

/**
 * 无真实手机号客户：库存 UK 列要求非空，使用 X 前缀 + 19 位数字占位（总长 20，匹配离线库 phone VARCHAR(20)）。
 */
public final class SyntheticMemberPhone {

    private static final String PREFIX = "X";

    private SyntheticMemberPhone() {}

    public static boolean isSynthetic(String phone) {
        return phone != null && phone.startsWith(PREFIX);
    }

    public static String next() {
        return PREFIX + RandomUtil.randomNumbers(19);
    }
}
