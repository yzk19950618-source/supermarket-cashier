-- ============================================
-- 化肥收银管理系统 数据库初始化脚本
-- 数据库：cashier_db
-- ============================================

CREATE DATABASE IF NOT EXISTS `cashier_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `cashier_db`;

-- ----------------------------
-- 1. 系统用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名/工号',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-收银员 1-管理员',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- 2. 商品分类表
-- ----------------------------
DROP TABLE IF EXISTS `goods_category`;
CREATE TABLE `goods_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID（0为顶级分类）',
  `sort` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ----------------------------
-- 3. 商品表
-- ----------------------------
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `barcode` VARCHAR(50) NOT NULL COMMENT '商品条码',
  `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `name_initial` VARCHAR(100) DEFAULT NULL COMMENT '名称首字母（用于首字母搜索）',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `unit` VARCHAR(10) DEFAULT '个' COMMENT '单位',
  `purchase_price` DECIMAL(10,2) NOT NULL COMMENT '进货价',
  `selling_price` DECIMAL(10,2) NOT NULL COMMENT '零售价',
  `stock` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '库存数量(可小数)',
  `stock_warning` DECIMAL(12,3) DEFAULT 10 COMMENT '库存预警值(可小数)',
  `image` VARCHAR(255) DEFAULT NULL COMMENT '商品图片URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-上架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_barcode` (`barcode`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ----------------------------
-- 4. 会员表
-- ----------------------------
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '客户姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '会员余额',
  `points` INT DEFAULT 0 COMMENT '积分',
  `discount` DECIMAL(3,2) DEFAULT 1.00 COMMENT '会员折扣',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

-- ----------------------------
-- 5. 销售订单表
-- ----------------------------
DROP TABLE IF EXISTS `sale_order`;
CREATE TABLE `sale_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `member_id` BIGINT DEFAULT NULL COMMENT '会员ID',
  `customer_name` VARCHAR(50) NOT NULL COMMENT '客户姓名',
  `customer_phone` VARCHAR(20) NOT NULL COMMENT '客户电话',
  `customer_address` VARCHAR(255) DEFAULT NULL COMMENT '客户地址',
  `customer_gender` TINYINT DEFAULT 0 COMMENT '客户性别',
  `repay_date` DATE NOT NULL COMMENT '还款日期',
  `delivery_date` DATE NOT NULL COMMENT '送货日期',
  `user_id` BIGINT NOT NULL COMMENT '收银员ID',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额',
  `real_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `pay_type` TINYINT DEFAULT NULL COMMENT '支付方式：0-现金 1-微信 2-支付宝 3-会员余额 4-银行卡',
  `status` TINYINT NOT NULL DEFAULT 2 COMMENT '订单状态：0-已退款 1-已支付 2-未支付',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `order_date` DATE NOT NULL COMMENT '订单日期',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `paid_time` DATETIME DEFAULT NULL COMMENT '核销时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单表';

-- ----------------------------
-- 6. 订单明细表
-- ----------------------------
DROP TABLE IF EXISTS `sale_order_item`;
CREATE TABLE `sale_order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `goods_id` BIGINT NOT NULL COMMENT '商品ID',
  `goods_name` VARCHAR(100) NOT NULL COMMENT '商品名称（冗余）',
  `barcode` VARCHAR(50) DEFAULT NULL COMMENT '商品条码（冗余）',
  `category_name` VARCHAR(100) NOT NULL COMMENT '商品品类（冗余）',
  `selling_price` DECIMAL(10,2) NOT NULL COMMENT '销售单价',
  `quantity` DECIMAL(12,3) NOT NULL COMMENT '购买数量(可小数)',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ----------------------------
-- 6.1 订单分批还款 / 附件 / 操作日志（与 OrderExtraSchemaInitializer 一致；应用启动亦会 CREATE IF NOT EXISTS）
-- ----------------------------
DROP TABLE IF EXISTS `sale_order_operation_log`;
DROP TABLE IF EXISTS `sale_order_attachment`;
DROP TABLE IF EXISTS `sale_order_repayment`;

CREATE TABLE IF NOT EXISTS `sale_order_repayment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `pay_type` INT NOT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单分批还款';

CREATE TABLE IF NOT EXISTS `sale_order_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `attachment_type` INT NOT NULL,
  `url` VARCHAR(255) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单附件';

CREATE TABLE IF NOT EXISTS `sale_order_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `operator_id` BIGINT NOT NULL,
  `operation_type` VARCHAR(50) NOT NULL,
  `payload_json` TEXT,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作日志';

-- ----------------------------
-- 7. 供应商表
-- ----------------------------
DROP TABLE IF EXISTS `supplier`;
CREATE TABLE `supplier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
  `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- ----------------------------
-- 8. 进货记录表
-- ----------------------------
DROP TABLE IF EXISTS `purchase_record`;
CREATE TABLE `purchase_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `purchase_no` VARCHAR(50) NOT NULL COMMENT '进货单号',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `goods_id` BIGINT NOT NULL COMMENT '商品ID',
  `quantity` INT NOT NULL COMMENT '进货数量',
  `purchase_price` DECIMAL(10,2) NOT NULL COMMENT '进货单价',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
  `user_id` BIGINT NOT NULL COMMENT '操作人ID',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进货记录表';

-- ============================================
-- 初始化数据
-- ============================================

-- 管理员账号（密码：123456，BCrypt加密）
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `role`, `status`) VALUES
('admin', '$2a$10$s5NZkNqda.FmtQhQdFFfUeU5ZZN.C8X0CdlBaW6sp0Ly0RZrVbJuC', '系统管理员', '13800000001', 1, 1);

-- 收银员账号（密码：123456）
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `role`, `status`) VALUES
('cashier01', '$2a$10$s5NZkNqda.FmtQhQdFFfUeU5ZZN.C8X0CdlBaW6sp0Ly0RZrVbJuC', '收银员小王', '13800000002', 0, 1),
('cashier02', '$2a$10$s5NZkNqda.FmtQhQdFFfUeU5ZZN.C8X0CdlBaW6sp0Ly0RZrVbJuC', '收银员小李', '13800000003', 0, 1);

-- 商品分类
INSERT INTO `goods_category` (`name`, `parent_id`, `sort`, `status`) VALUES
('氮肥', 0, 1, 1),
('磷肥', 0, 2, 1),
('钾肥', 0, 3, 1),
('复合肥', 0, 4, 1),
('水溶肥', 0, 5, 1),
('微量元素', 0, 6, 1),
('有机肥', 0, 7, 1),
('生物菌肥', 0, 8, 1);

-- 商品数据
INSERT INTO `goods` (`barcode`, `name`, `name_initial`, `category_id`, `unit`, `purchase_price`, `selling_price`, `stock`, `stock_warning`) VALUES
('FERT0000000001', '尿素 46% 50kg（袋装）', 'NS', 1, '袋', 118.00, 138.00, 260, 30),
('FERT0000000002', '硝酸铵钙 25kg（袋装）', 'XSAG', 1, '袋', 86.00, 99.00, 180, 25),
('FERT0000000003', '磷酸二铵 18-46-0 50kg', 'LSEA', 2, '袋', 228.00, 258.00, 120, 20),
('FERT0000000004', '过磷酸钙 40kg（袋装）', 'GLSG', 2, '袋', 72.00, 88.00, 140, 20),
('FERT0000000005', '氯化钾 60% 50kg（袋装）', 'LHJ', 3, '袋', 258.00, 298.00, 90, 15),
('FERT0000000006', '硫酸钾 50% 50kg（袋装）', 'LSJ', 3, '袋', 318.00, 358.00, 75, 12),
('FERT0000000007', '复合肥 15-15-15 50kg', 'FHF', 4, '袋', 168.00, 198.00, 200, 25),
('FERT0000000008', '复合肥 17-17-17 50kg', 'FHF', 4, '袋', 178.00, 208.00, 160, 22),
('FERT0000000009', '大量元素水溶肥 20-20-20 10kg', 'DLYSSRF', 5, '箱', 138.00, 168.00, 60, 10),
('FERT0000000010', '高钾水溶肥 12-6-40 10kg', 'GJSRF', 5, '箱', 148.00, 178.00, 55, 10),
('FERT0000000011', '硼肥 1kg（袋装）', 'BF', 6, '袋', 18.00, 25.00, 220, 30),
('FERT0000000012', '锌肥 1kg（袋装）', 'XF', 6, '袋', 20.00, 28.00, 200, 30),
('FERT0000000013', '颗粒有机肥 40kg（袋装）', 'KLYJF', 7, '袋', 68.00, 88.00, 130, 18),
('FERT0000000014', '生物菌肥 20kg（袋装）', 'SWJF', 8, '袋', 78.00, 98.00, 110, 18);

-- 客户会员数据
INSERT INTO `member` (`name`, `phone`, `gender`, `address`, `remark`, `balance`, `points`, `discount`) VALUES
('惠农合作社', '13900001111', 0, '河南省周口市淮阳区朱集镇', '长期合作客户，月底结算', 800.00, 2600, 0.95),
('绿田家庭农场', '13900002222', 0, '山东省菏泽市曹县青岗集镇', '玉米种植户，旺季批量采购', 300.00, 900, 0.98),
('丰收种植大户', '13900003333', 0, '河北省邯郸市成安县商城镇', '支持赊销，注意回款节点', 1200.00, 4200, 0.92);

-- 供应商数据
INSERT INTO `supplier` (`name`, `contact`, `phone`, `address`) VALUES
('河南中原化肥有限公司', '陈经理', '0371-88881111', '河南省郑州市经开区化工路8号'),
('云南磷化集团供应链', '刘经理', '0871-22223333', '云南省昆明市呈贡区产业园B区'),
('青岛钾肥贸易有限公司', '王经理', '0532-55556666', '山东省青岛市黄岛区港城大道66号');
