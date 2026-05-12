# 增量 SQL（上线库 / 生产库）

仓库 **`init.sql`** 与离线全量包中的全量建库脚本保持一致；**本目录不包含全量 init**，仅提供与当前应用版本对齐的**统一幂等增量**。

## 唯一执行入口

| 文件 | 说明 |
|------|------|
| **`run_all_incremental.sql`** | 合并全部上线所需 DDL/DML；按 `information_schema` 判断缺列再 `ADD`，可**重复执行**，不依赖执行顺序、不误删业务数据。 |

内容概要：

1. **`member.remark` → VARCHAR(2000)**：仅放宽长度。
2. **`goods.promo_*`**：同款买满送字段（不存在则添加）。
3. **`sale_order_item.is_gift`**：赠品标记（不存在则添加）。
4. **`member.discount`**：仅将异常值（NULL / <0 / >1）更新为 `1.00`。
5. **去掉 `goods.barcode`**：保留在 `run_all_incremental.sql` 末尾**注释块**内；仅异常历史库在评估后手工取消注释执行。

执行前请备份；`USE` 库名按环境修改。

## Windows 离线目录

- **唯一一份增量 SQL**：离线包根目录下 **`db\incremental\run_all_incremental.sql`**（本仓库对应 `cashier-backend/src/main/resources/db/incremental/`）。
- **执行**：根目录 **`dbsql\run-incremental.bat`**（从 `..\db\incremental\` 读取上述文件；不复制第二份到 dbsql）。

## region-init

与离线包 `region-init.sql` 一致，无额外增量。
