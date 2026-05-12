@echo off
setlocal
chcp 65001 >nul

rem 本目录 dbsql 与 offline 根目录下的 mysql、scripts 同级
set "HERE=%~dp0"
for %%I in ("%HERE%..") do set "APP_HOME=%%~fI"

set "MYSQL_ROOT=%APP_HOME%\mysql"
set "MYSQL_HOME=%MYSQL_ROOT%"
if not exist "%MYSQL_HOME%\bin\mysql.exe" (
  for /d %%D in ("%MYSQL_ROOT%\*") do (
    if exist "%%~fD\bin\mysql.exe" set "MYSQL_HOME=%%~fD"
  )
)

if not exist "%MYSQL_HOME%\bin\mysql.exe" (
  echo [ERROR] 未找到 mysql.exe，请确认离线包内已解压 MySQL 到: "%MYSQL_ROOT%"
  exit /b 1
)

"%MYSQL_HOME%\bin\mysqladmin.exe" -uroot -proot ping >nul 2>nul
if errorlevel 1 (
  echo [ERROR] MySQL 未启动或 root 密码不是离线默认。请先执行 scripts\runtime\start.bat 启动后再运行本脚本。
  exit /b 1
)

dir /b /o:n "%HERE%*.sql" 2>nul | findstr /r "." >nul
if errorlevel 1 (
  echo [WARN] 本目录下没有 .sql 文件，无需执行。
  exit /b 0
)

echo [INFO] 将按文件名顺序执行本目录内所有 .sql（目标库见各脚本内 USE 语句，一般为 cashier_db）
for /f "delims=" %%F in ('dir /b /o:n "%HERE%*.sql"') do (
  echo [INFO] 正在执行: %%F
  "%MYSQL_HOME%\bin\mysql.exe" --default-character-set=utf8mb4 -uroot -proot cashier_db < "%HERE%%%F"
  if errorlevel 1 (
    echo [ERROR] 执行失败: %%F
    exit /b 1
  )
)

echo [OK] dbsql 内增量 SQL 已全部执行完毕。
exit /b 0
