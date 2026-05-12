# 增量 SQL（上线库）

仓库 **`init.sql` 与离线安装包 `supermarket-cashier_v1.0.1_win64_offline\db\init.sql` 保持一致**，不在此目录重复维护全量结构。

## 统一增量脚本

| 文件 | 说明 |
|------|------|
| **`post_offline_v1.0.1_schema_upgrade.sql`** | 离线 init 之后、与**当前应用**对齐的**唯一**增量脚本（幂等） |

内容概要：

1. **`member.remark` → VARCHAR(2000)**：默认执行；只放宽字段，不删改业务行。
2. **`goods` 去掉 `barcode`**：脚本内**默认整段注释**。当前应用已与离线 `init`（含主表 `barcode`）一致，**勿执行**；仅适用于曾手工删除该列的异常库（见脚本内说明）。

执行前备份；`USE` 库名按环境修改。

离线 Windows 包内另有 **`dbsql\run-incremental.bat`**：在已启动本包 MySQL 的前提下，可对本目录同步过去的 `.sql` 逐条执行（无需单独安装 SQL 客户端）。

## region-init

与离线包 `region-init.sql` 一致，无额外增量。
