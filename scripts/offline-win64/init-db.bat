@echo off
setlocal
chcp 65001 >nul

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "APP_HOME=%%~fI"
if not exist "%APP_HOME%\db\init.sql" (
  for %%I in ("%SCRIPT_DIR%..\..") do set "APP_HOME=%%~fI"
)

set "MYSQL_ROOT=%APP_HOME%\mysql"
set "DATA_DIR=%APP_HOME%\data\mysql"
set "LOG_DIR=%APP_HOME%\logs"
set "INIT_SQL=%APP_HOME%\db\init.sql"

set "MYSQL_HOME=%MYSQL_ROOT%"
if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
  for /d %%D in ("%MYSQL_ROOT%\*") do (
    if exist "%%~fD\bin\mysqld.exe" set "MYSQL_HOME=%%~fD"
  )
)

set "MY_INI=%MYSQL_ROOT%\my.ini"
if not exist "%MY_INI%" set "MY_INI=%MYSQL_HOME%\my.ini"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>nul
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%" >nul 2>nul

if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
  echo [ERROR] MySQL not found. Expected mysqld.exe under: "%MYSQL_ROOT%"
  exit /b 1
)

if not exist "%INIT_SQL%" (
  echo [ERROR] init.sql not found: "%INIT_SQL%"
  echo         请从您的离线备份将 init.sql 复制到 db\ 后再运行。
  echo         仅升级已有库时，请直接在 MySQL 中执行 db\incremental\ 下的 SQL。
  exit /b 1
)

set "NEED_INIT=0"
for /f %%A in ('dir /a /b "%DATA_DIR%" 2^>nul ^| find /c /v ""') do set "CNT=%%A"
if "%CNT%"=="0" set "NEED_INIT=1"

if "%NEED_INIT%"=="1" (
  echo [INFO] Initializing MySQL data dir...
  "%MYSQL_HOME%\bin\mysqld.exe" --basedir="%MYSQL_HOME%" --datadir="%DATA_DIR%" --initialize-insecure --console > "%LOG_DIR%\mysql-init.log" 2>&1
  if errorlevel 1 (
    echo [ERROR] MySQL initialize failed. Check: "%LOG_DIR%\mysql-init.log"
    exit /b 1
  )
) else (
  echo [INFO] MySQL data dir already initialized.
)

echo [INFO] Starting MySQL...
start "mysql" /B "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%MY_INI%" --basedir="%MYSQL_HOME%" --datadir="%DATA_DIR%" --console > "%LOG_DIR%\mysql.log" 2>&1

set "READY=0"
for /l %%i in (1,1,30) do (
  "%MYSQL_HOME%\bin\mysqladmin.exe" -uroot ping >nul 2>nul
  if not errorlevel 1 (
    set "READY=1"
    goto :ready
  )
  ping 127.0.0.1 -n 2 >nul
)
:ready
if "%READY%"=="0" (
  echo [ERROR] MySQL not responding on time. Check: "%LOG_DIR%\mysql.log"
  exit /b 1
)

echo [INFO] Setting MySQL root password...
"%MYSQL_HOME%\bin\mysqladmin.exe" -uroot password root >nul 2>nul

echo [INFO] Creating database/user and importing init.sql...
"%MYSQL_HOME%\bin\mysql.exe" --default-character-set=utf8mb4 -uroot -proot -e "CREATE DATABASE IF NOT EXISTS cashier_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; CREATE USER IF NOT EXISTS 'cashier'@'127.0.0.1' IDENTIFIED BY 'cashier123'; CREATE USER IF NOT EXISTS 'cashier'@'localhost' IDENTIFIED BY 'cashier123'; GRANT ALL PRIVILEGES ON cashier_db.* TO 'cashier'@'127.0.0.1'; GRANT ALL PRIVILEGES ON cashier_db.* TO 'cashier'@'localhost'; FLUSH PRIVILEGES;" >nul
"%MYSQL_HOME%\bin\mysql.exe" --default-character-set=utf8mb4 -uroot -proot < "%INIT_SQL%"
if errorlevel 1 (
  echo [ERROR] init.sql import failed.
  exit /b 1
)

echo [OK] Database initialized.
exit /b 0
