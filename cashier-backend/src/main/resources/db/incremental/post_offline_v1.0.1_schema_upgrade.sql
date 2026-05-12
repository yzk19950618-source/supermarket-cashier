-- =============================================================================
-- 离线包 init 之后的统一增量（与 supermarket-cashier_v1.0.1_win64_offline\db\init.sql 对比当前应用）
--
-- 仓库内 init.sql 与离线版保持一致；上线库执行本文件（可按需只执行第 1 段）。
--
-- 第 1 段：member.remark 扩至 VARCHAR(2000) — 仅放宽长度，不删行、不截断已有内容。
--
-- 第 2 段（默认整段注释）：删除 goods.barcode / uk_barcode。
--         当前后端已与离线 init 对齐（含主表 barcode），正常部署不要执行第 2 段。
--         仅当历史库曾手工去掉主表 barcode、且确认不再需要该列时，可取消注释执行
--         （会丢失主表 barcode 列数据；订单明细 sale_order_item.barcode 不受影响）。
--
-- 执行前请备份；USE 库名按实际修改。
-- =============================================================================

USE `cashier_db`;

-- ---------------------------------------------------------------------------
-- 第 1 段：会员备注扩容（推荐；幂等）
-- ---------------------------------------------------------------------------
ALTER TABLE `member`
  MODIFY COLUMN `remark` VARCHAR(2000) DEFAULT NULL COMMENT '备注';

-- ---------------------------------------------------------------------------
-- 第 2 段（极少用）：去掉 goods 主表 barcode
-- 需要执行时：删除本段首尾 /* 与 */，再执行整个脚本或单独执行段内语句。
-- ---------------------------------------------------------------------------
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
