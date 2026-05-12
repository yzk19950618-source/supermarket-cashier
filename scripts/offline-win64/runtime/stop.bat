@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "APP_HOME=%%~fI"
if not exist "%APP_HOME%\backend\cashier-backend.jar" (
  for %%I in ("%SCRIPT_DIR%..\..") do set "APP_HOME=%%~fI"
)
set "MYSQL_HOME=%APP_HOME%\mysql"

echo [INFO] Stopping backend...
powershell -NoProfile -Command "$p = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*cashier-backend.jar*' -and $_.ExecutablePath -like '*java*' }; if ($p) { $p | ForEach-Object { Stop-Process -Id $_.ProcessId -Force } }"

echo [INFO] Stopping MySQL...
if exist "%MYSQL_HOME%\bin\mysqladmin.exe" (
  "%MYSQL_HOME%\bin\mysqladmin.exe" -uroot -proot shutdown >nul 2>nul
)

echo [OK] Stopped.
exit /b 0
