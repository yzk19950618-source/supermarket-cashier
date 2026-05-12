本目录只放「执行增量」的 bat，真正的 SQL 只在上一级的 db\incremental\ 里（一份 run_all_incremental.sql），避免 dbsql 与 db 两处各塞一套脚本。

前提：
  1. 已启动本包 MySQL（例如 scripts\runtime\start.bat）。
  2. 离线根目录下存在 db\incremental\run_all_incremental.sql（随发行 / 增量包或 scripts\pack-db-incremental.ps1 打出 zip 合并进来）。

操作：
  双击或在 cmd 中执行：run-incremental.bat

说明：
  - bat 内为英文提示，避免 GBK 控制台误解析 UTF-8。
  - 使用 root / 离线默认密码，对 cashier_db 执行上述 SQL（脚本内亦有 USE）。
  - 幂等，可重复执行。
  - 执行前请备份 cashier_db。
