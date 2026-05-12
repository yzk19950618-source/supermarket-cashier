@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem Canonical SQL: ..\db\incremental\run_all_incremental.sql (single copy). Fallback: same folder as this bat.
set "SQLFILE=%~dp0..\db\incremental\run_all_incremental.sql"
if not exist "%SQLFILE%" set "SQLFILE=%~dp0run_all_incremental.sql"
if not exist "%SQLFILE%" (
  echo [ERROR] run_all_incremental.sql not found. Tried:
  echo   "%~dp0..\db\incremental\run_all_incremental.sql"
  echo   "%~dp0run_all_incremental.sql"
  echo Ensure db\incremental\run_all_incremental.sql exists under offline root, or run scripts\pack-db-incremental.ps1 from repo.
  exit /b 1
)
for %%I in ("%SQLFILE%") do set "SQLFILE=%%~fI"

pushd "%~dp0.."
set "APP_HOME=%CD%"
popd
cd /d "%~dp0"

set "MYSQL_ROOT=%APP_HOME%\mysql"
set "MYSQL_HOME=%MYSQL_ROOT%"
if not exist "%MYSQL_HOME%\bin\mysql.exe" (
  for /d %%D in ("%MYSQL_ROOT%\*") do (
    if exist "%%~fD\bin\mysql.exe" set "MYSQL_HOME=%%~fD"
  )
)

if not exist "%MYSQL_HOME%\bin\mysql.exe" (
  echo [ERROR] mysql.exe not found. Expected under: "%MYSQL_ROOT%"
  exit /b 1
)

"%MYSQL_HOME%\bin\mysqladmin.exe" -uroot -proot ping >nul 2>nul
if errorlevel 1 (
  echo [ERROR] MySQL not running or root password is not the offline default. Start MySQL first ^(e.g. scripts\runtime\start.bat^).
  exit /b 1
)

echo [INFO] Using SQL file: %SQLFILE%
echo [INFO] Applying to database cashier_db ^(idempotent^) ...
"%MYSQL_HOME%\bin\mysql.exe" --default-character-set=utf8mb4 -uroot -proot cashier_db < "%SQLFILE%"
if errorlevel 1 (
  echo [ERROR] run_all_incremental.sql failed.
  exit /b 1
)

echo [OK] Done.
exit /b 0
