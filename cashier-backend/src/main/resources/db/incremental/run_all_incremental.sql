-- =============================================================================
-- 统一增量脚本（幂等）：上线 / 生产库可重复执行；仅「缺列则加、仅异常数据则改」。
-- 不包含全量建库；与 init.sql 无关。
--
-- 执行前请备份；按需修改 USE 库名（与 mysql 客户端所选库一致即可）。
-- =============================================================================

USE `cashier_db`;

-- ---------------------------------------------------------------------------
-- 1) 会员备注字段放宽（重复执行安全，不截断已有内容）
-- ---------------------------------------------------------------------------
ALTER TABLE `member`
  MODIFY COLUMN `remark` VARCHAR(2000) DEFAULT NULL COMMENT '备注';

-- ---------------------------------------------------------------------------
-- 2) goods：同款买满送配置列（按列存在性跳过，已上线库可反复执行）
-- ---------------------------------------------------------------------------
SET @__sql := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'goods' AND COLUMN_NAME = 'promo_enabled'),
    'SELECT ''skip: goods.promo_enabled'' AS `_note`',
    'ALTER TABLE `goods` ADD COLUMN `promo_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT ''同款买赠：1启用'' AFTER `status`'
  )
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__sql := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'goods' AND COLUMN_NAME = 'promo_buy_qty'),
    'SELECT ''skip: goods.promo_buy_qty'' AS `_note`',
    'ALTER TABLE `goods` ADD COLUMN `promo_buy_qty` INT DEFAULT NULL COMMENT ''满多少件触发'' AFTER `promo_enabled`'
  )
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__sql := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'goods' AND COLUMN_NAME = 'promo_gift_qty'),
    'SELECT ''skip: goods.promo_gift_qty'' AS `_note`',
    'ALTER TABLE `goods` ADD COLUMN `promo_gift_qty` INT DEFAULT NULL COMMENT ''送多少件同款'' AFTER `promo_buy_qty`'
  )
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

-- ---------------------------------------------------------------------------
-- 3) sale_order_item：赠品标记（按列存在性跳过）
-- ---------------------------------------------------------------------------
SET @__sql := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sale_order_item' AND COLUMN_NAME = 'is_gift'),
    'SELECT ''skip: sale_order_item.is_gift'' AS `_note`',
    'ALTER TABLE `sale_order_item` ADD COLUMN `is_gift` TINYINT NOT NULL DEFAULT 0 COMMENT ''1=活动赠品'' AFTER `subtotal`'
  )
);
PREPARE __stmt FROM @__sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

-- ---------------------------------------------------------------------------
-- 4) 会员 discount 列异常值归一（应用层不再使用折扣率语义；仅更新异常行）
-- ---------------------------------------------------------------------------
UPDATE `member`
SET `discount` = 1.00
WHERE `deleted` = 0
  AND (`discount` IS NULL OR `discount` < 0 OR `discount` > 1);

-- ---------------------------------------------------------------------------
-- 5) 订单明细数量、商品库存与买赠「买满 / 送」支持小数（如 0.5 袋；可重复执行）
--    须在本文件第 2) 节已为 goods 增加 promo_* 列之后执行（正常顺序已满足）。
-- ---------------------------------------------------------------------------
ALTER TABLE `sale_order_item`
  MODIFY COLUMN `quantity` DECIMAL(12,3) NOT NULL COMMENT '数量(可小数)';

ALTER TABLE `goods`
  MODIFY COLUMN `stock` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '库存数量';

ALTER TABLE `goods`
  MODIFY COLUMN `stock_warning` DECIMAL(12,3) DEFAULT 10 COMMENT '库存预警值';

ALTER TABLE `goods`
  MODIFY COLUMN `promo_buy_qty` DECIMAL(12,3) DEFAULT NULL COMMENT '满多少触发(可小数)';

ALTER TABLE `goods`
  MODIFY COLUMN `promo_gift_qty` DECIMAL(12,3) DEFAULT NULL COMMENT '送多少同款(可小数)';

-- =============================================================================
-- 以下整段保持注释：仅当历史库曾手工去掉 goods.barcode 且确认不再需要时，
-- 再取消注释单独执行（正常生产 / 与当前 init 对齐的库切勿执行）。
-- =============================================================================
/*
SET @db = DATABASE();

SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = @db AND table_name = 'goods' AND index_name = 'uk_barcode';

SET @q1 = IF(@idx_exists > 0,
    'ALTER TABLE `goods` DROP INDEX `uk_barcode`',
    'SELECT ''skip: goods.uk_barcode absent'' AS `_note`');
PREPARE ps1 FROM @q1;
EXECUTE ps1;
DEALLOCATE PREPARE ps1;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = @db AND table_name = 'goods' AND column_name = 'barcode';

SET @q2 = IF(@col_exists > 0,
    'ALTER TABLE `goods` DROP COLUMN `barcode`',
    'SELECT ''skip: goods.barcode absent'' AS `_note`');
PREPARE ps2 FROM @q2;
EXECUTE ps2;
DEALLOCATE PREPARE ps2;
*/
