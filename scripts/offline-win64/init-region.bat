@echo off
setlocal
chcp 65001 >nul
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\") do set "APP_HOME=%%~fI"
if not exist "%APP_HOME%\db\region-init.sql" (
  for %%I in ("%SCRIPT_DIR%..\..\") do set "APP_HOME=%%~fI"
)
set "MYSQL_ROOT=%APP_HOME%\mysql"
set "MYSQL_HOME=%MYSQL_ROOT%"
set "REGION_SQL=%APP_HOME%\db\region-init.sql"

if not exist "%MYSQL_HOME%\bin\mysql.exe" (
  for /d %%D in ("%MYSQL_ROOT%\*") do (
    if exist "%%~fD\bin\mysql.exe" set "MYSQL_HOME=%%~fD"
  )
)

if not exist "%MYSQL_HOME%\bin\mysql.exe" (
  echo [ERROR] mysql.exe not found under %MYSQL_ROOT%
  exit /b 1
)
if not exist "%REGION_SQL%" (
  echo [ERROR] region-init.sql not found: %REGION_SQL%
  echo         请从您的离线备份将 region-init.sql 复制到 db\ 后再运行。
  exit /b 1
)

"%MYSQL_HOME%\bin\mysqladmin.exe" -uroot -proot ping >nul 2>nul
if errorlevel 1 (
  echo [ERROR] MySQL is not running. Please run scripts\init\init-db.bat first.
  exit /b 1
)

echo [INFO] Importing offline region data...
"%MYSQL_HOME%\bin\mysql.exe" --default-character-set=utf8mb4 -uroot -proot cashier_db < "%REGION_SQL%"
if errorlevel 1 (
  echo [ERROR] Region import failed.
  exit /b 1
)
echo [OK] Region import done.
exit /b 0
