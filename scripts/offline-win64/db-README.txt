本目录随发行包仅附带「增量」SQL（见 incremental\），不含全量 init。

首次安装（空库）
  1. 将您备份中的 init.sql、region-init.sql 复制到本目录 db\ 根下（与 scripts\init 脚本约定路径一致）。
  2. 依次执行 scripts\init\init-db.bat、init-region.bat（或 init-all.bat）。

已有库升级
  任选：1) 启动本包 MySQL 后双击 dbsql\run-incremental.bat；2) 在 MySQL 中按需执行 incremental\ 下脚本（执行前请备份；按脚本内说明修改 USE 库名）。
