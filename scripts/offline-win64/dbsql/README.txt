本目录用于「无图形化 SQL 工具」时，在 Windows 下用离线包自带的 mysql 客户端执行增量脚本。

前提：
  1. 已按离线说明解压 JRE、MySQL，且至少成功启动过一次 MySQL（例如执行过 scripts\runtime\start.bat）。
  2. 本目录下应带有从发行包同步的 .sql 文件（与 db\incremental 内容一致）。

操作：
  双击或在 cmd 中执行：run-incremental.bat

说明：
  - 使用 root / 离线默认密码连接本机 MySQL，并对 cashier_db 执行管道输入（与各脚本内 USE 一致）。
  - 按文件名排序依次执行本目录内全部 .sql；任一步失败会中止。
  - 执行前请自行备份 cashier_db。
