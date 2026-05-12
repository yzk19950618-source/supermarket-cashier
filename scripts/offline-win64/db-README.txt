本目录随发行包：**全量** init 仍由您自备 `init.sql`、`region-init.sql` 放到 `db\` 根下（首次安装）；**增量**只维护一处：`db\incremental\run_all_incremental.sql`（幂等）。

首次安装（空库）
  1. 将 init.sql、region-init.sql 复制到本目录 db\ 根下。
  2. 依次执行 scripts\init\init-db.bat、init-region.bat（或 init-all.bat）。

已有库升级
  1) 启动 MySQL 后双击 dbsql\run-incremental.bat（从 db\incremental\ 读 SQL）；或 2) 用客户端执行 db\incremental\run_all_incremental.sql。
