package com.cashier.common.utils;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.IdUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 订单编号生成工具类
 *
 * @author cashier
 * @since 2024-01-01
 */
public class OrderNoUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 生成订单编号
     * 格式：SO + 年月日时分秒 + 4位随机数
     * 示例：SO202401011200001234
     */
    public static String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String random = IdUtil.randomUUID().substring(0, 4).toUpperCase();
        return "SO" + timestamp + random;
    }

    /**
     * 生成进货单号
     * 格式：PO + 年月日时分秒 + 4位随机数
     */
    public static String generatePurchaseNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String random = IdUtil.randomUUID().substring(0, 4).toUpperCase();
        return "PO" + timestamp + random;
    }
}
