-- 订单收货地址与附件（发票/凭证图片 URL 列表，JSON 数组文本）
-- 已部署库请手动执行本脚本一次

ALTER TABLE `sale_order`
    ADD COLUMN `receiver_address` VARCHAR(512) DEFAULT NULL COMMENT '收货详细地址（含省市区展示文案）' AFTER `remark`,
    ADD COLUMN `receiver_region_codes` VARCHAR(128) DEFAULT NULL COMMENT '省市区编码，逗号分隔' AFTER `receiver_address`,
    ADD COLUMN `attachment_urls` TEXT DEFAULT NULL COMMENT '附件图片 URL 列表（JSON 数组字符串）' AFTER `receiver_region_codes`;
