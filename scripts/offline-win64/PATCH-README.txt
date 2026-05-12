================================================================================
已有离线安装目录时 — 本次增量需要替换的内容
================================================================================

解压本压缩包到「离线包根目录」（与 backend、frontend、db 同级），选择覆盖同名路径。

【必换 — 应用代码】
  1) backend\cashier-backend.jar
     先执行 scripts\runtime\stop.bat 停掉后端与 MySQL（若仅需换 jar 可先只停 Java），
     再覆盖该 jar。

  2) frontend\dist\  （整个目录）
     删除或清空离线包内旧的 frontend\dist，将本包内 frontend\dist 全部复制进去。
     （start.bat 会从该目录加载静态资源；jar 内也带一份，两处一致即可。）

【必看 — 数据库】
  3) db\incremental\ 与 dbsql\
     增量 SQL **只保留一份**：db\incremental\run_all_incremental.sql。dbsql\ 内仅有 run-incremental.bat（从 ..\db\incremental\ 读取该 SQL）。
     不会自动执行。请先备份 cashier_db，再任选：
       A) 双击 dbsql\run-incremental.bat（需已启动本包 MySQL）
       B) 自行用客户端执行 db\incremental\run_all_incremental.sql（阅读同目录 README.md）

     说明：增量包**不包含**全量 init；请勿对空库当结构初始化用，仅用于**已有** cashier_db 幂等升级。

【可选 — 脚本】
  4) scripts\runtime\start.bat、stop.bat
     建议覆盖：修复旧版 start.bat 在 for 块内 goto 导致卡住的问题。

【推荐顺序】
  stop -> 覆盖 jar 与 frontend\dist -> start（起 MySQL）->（按需）dbsql\run-incremental.bat -> 如已停后端可再 start

================================================================================
