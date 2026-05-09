-- ============================================
-- 超市收银系统 数据库初始化脚本
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
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `unit` VARCHAR(10) DEFAULT '个' COMMENT '单位',
  `purchase_price` DECIMAL(10,2) NOT NULL COMMENT '进货价',
  `selling_price` DECIMAL(10,2) NOT NULL COMMENT '零售价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `stock_warning` INT DEFAULT 10 COMMENT '库存预警值',
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
  `card_no` VARCHAR(50) NOT NULL COMMENT '会员卡号',
  `name` VARCHAR(50) NOT NULL COMMENT '会员姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
  `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '会员余额',
  `points` INT DEFAULT 0 COMMENT '积分',
  `discount` DECIMAL(3,2) DEFAULT 1.00 COMMENT '会员折扣',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_card_no` (`card_no`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

-- ----------------------------
-- 5. 销售订单表
-- ----------------------------
DROP TABLE IF EXISTS `sale_order`;
CREATE TABLE `sale_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `member_id` BIGINT DEFAULT NULL COMMENT '会员ID',
  `user_id` BIGINT NOT NULL COMMENT '收银员ID',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额',
  `real_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `pay_type` TINYINT NOT NULL COMMENT '支付方式：0-现金 1-微信 2-支付宝 3-会员余额 4-银行卡',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '订单状态：0-已退款 1-已完成',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `receiver_address` VARCHAR(512) DEFAULT NULL COMMENT '收货详细地址',
  `receiver_region_codes` VARCHAR(128) DEFAULT NULL COMMENT '省市区编码逗号分隔',
  `attachment_urls` TEXT DEFAULT NULL COMMENT '附件图片URL列表JSON',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
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
  `barcode` VARCHAR(50) NOT NULL COMMENT '商品条码（冗余）',
  `selling_price` DECIMAL(10,2) NOT NULL COMMENT '销售单价',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

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
('admin', '$2a$10$N.ZOn9MHFb8MERlyBM4Sje4WlYF43CtTRPMsi5aVEEDlI3bELDXKe', '系统管理员', '13800000001', 1, 1);

-- 收银员账号（密码：123456）
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `role`, `status`) VALUES
('cashier01', '$2a$10$N.ZOn9MHFb8MERlyBM4Sje4WlYF43CtTRPMsi5aVEEDlI3bELDXKe', '收银员小王', '13800000002', 0, 1),
('cashier02', '$2a$10$N.ZOn9MHFb8MERlyBM4Sje4WlYF43CtTRPMsi5aVEEDlI3bELDXKe', '收银员小李', '13800000003', 0, 1);

-- 商品分类
INSERT INTO `goods_category` (`name`, `parent_id`, `sort`, `status`) VALUES
('食品饮料', 0, 1, 1),
('日用百货', 0, 2, 1),
('生鲜果蔬', 0, 3, 1),
('休闲零食', 0, 4, 1),
('酒水饮料', 0, 5, 1),
('粮油调味', 0, 6, 1);

-- 商品数据
INSERT INTO `goods` (`barcode`, `name`, `category_id`, `unit`, `purchase_price`, `selling_price`, `stock`, `stock_warning`) VALUES
('6901028075831', '可口可乐330ml', 1, '瓶', 1.50, 3.00, 200, 20),
('6920459950180', '康师傅红烧牛肉面', 1, '袋', 2.00, 4.50, 150, 20),
('6925303731038', '蒙牛纯牛奶250ml', 1, '盒', 2.50, 5.00, 300, 30),
('6921168509256', '百事可乐500ml', 1, '瓶', 2.00, 3.50, 180, 20),
('6902538008074', '清风抽纸100抽', 2, '包', 3.00, 5.50, 120, 15),
('6920354825170', '黑人牙膏120g', 2, '支', 5.00, 9.90, 80, 10),
('6923450657713', '舒肤佳香皂125g', 2, '块', 3.50, 6.50, 100, 10),
('6911988015549', '怡宝矿泉水555ml', 5, '瓶', 0.80, 2.00, 500, 50),
('6901236341660', '旺旺雪饼84g', 4, '袋', 3.00, 5.80, 90, 10),
('6916189003536', '奥利奥饼干97g', 4, '包', 4.00, 7.50, 70, 10),
('6902083886028', '海天酱油500ml', 6, '瓶', 4.50, 8.90, 60, 10),
('6959014199425', '鲁花花生油1L', 6, '瓶', 18.00, 29.90, 40, 5),
('6921734900708', '伊利酸奶200g', 1, '杯', 2.50, 5.50, 120, 15),
('6925303720261', '特仑苏牛奶250ml', 1, '盒', 3.50, 6.90, 200, 20),
('6901668055323', '维达纸巾130抽', 2, '包', 3.50, 6.80, 150, 15);

-- 会员数据
INSERT INTO `member` (`card_no`, `name`, `phone`, `gender`, `balance`, `points`, `discount`) VALUES
('VIP20240001', '张三', '13900001111', 1, 500.00, 1200, 0.95),
('VIP20240002', '李四', '13900002222', 2, 200.00, 800, 0.98),
('VIP20240003', '王五', '13900003333', 1, 1000.00, 3500, 0.90);

-- 供应商数据
INSERT INTO `supplier` (`name`, `contact`, `phone`, `address`) VALUES
('广州食品批发有限公司', '陈经理', '020-88881111', '广州市白云区批发市场A栋'),
('深圳日化用品有限公司', '刘经理', '0755-22223333', '深圳市龙华区工业园B区'),
('东莞粮油贸易公司', '王经理', '0769-55556666', '东莞市厚街镇粮油市场');
