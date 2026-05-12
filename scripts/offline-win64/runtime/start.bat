@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "APP_HOME=%%~fI"
if not exist "%APP_HOME%\backend\cashier-backend.jar" (
  for %%I in ("%SCRIPT_DIR%..\..") do set "APP_HOME=%%~fI"
)

set "JRE_ROOT=%APP_HOME%\runtime\jre"
set "MYSQL_ROOT=%APP_HOME%\mysql"
set "DATA_DIR=%APP_HOME%\data\mysql"
set "LOG_DIR=%APP_HOME%\logs"
set "JAR=%APP_HOME%\backend\cashier-backend.jar"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>nul

set "JAVA_EXE=%JRE_ROOT%\bin\java.exe"
if not exist "%JAVA_EXE%" (
  for /d %%D in ("%JRE_ROOT%\*") do (
    if exist "%%~fD\bin\java.exe" set "JAVA_EXE=%%~fD\bin\java.exe"
  )
)

set "MYSQL_HOME=%MYSQL_ROOT%"
if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
  for /d %%D in ("%MYSQL_ROOT%\*") do (
    if exist "%%~fD\bin\mysqld.exe" set "MYSQL_HOME=%%~fD"
  )
)

if not exist "%JAVA_EXE%" (
  echo [ERROR] JRE not found. Expected java.exe under: "%JRE_ROOT%"
  exit /b 1
)

if not exist "%JAR%" (
  echo [ERROR] backend jar not found: "%JAR%"
  exit /b 1
)

if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
  echo [ERROR] MySQL not found. Expected mysqld.exe under: "%MYSQL_ROOT%"
  exit /b 1
)

if not exist "%DATA_DIR%" (
  echo [WARN] MySQL data dir not found, please run: scripts\init\init-db.bat
)

set "MY_INI=%MYSQL_ROOT%\my.ini"
if not exist "%MY_INI%" set "MY_INI=%MYSQL_HOME%\my.ini"

pushd "%APP_HOME%"

"%MYSQL_HOME%\bin\mysqladmin.exe" -uroot -proot ping >nul 2>nul
if errorlevel 1 (
  echo [INFO] Starting MySQL...
  start "mysql" /B "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%MY_INI%" --basedir="%MYSQL_HOME%" --datadir="%DATA_DIR%" --console > "%LOG_DIR%\mysql.log" 2>&1
  ping 127.0.0.1 -n 2 >nul
)

call :wait_mysql_up
if errorlevel 1 (
  echo [ERROR] MySQL start failed. Check logs\mysql.log
  popd
  exit /b 1
)

rem Optional: copy db-local.bat.example to db-local.bat to set DB_URL / DB_USERNAME / DB_PASSWORD
if exist "%SCRIPT_DIR%db-local.bat" call "%SCRIPT_DIR%db-local.bat"

echo [INFO] Starting backend on http://127.0.0.1:8080 ...
start "cashier-backend" /B "%JAVA_EXE%" -Dfile.encoding=UTF-8 -jar "%JAR%" --spring.profiles.active=offline --server.port=8080 --spring.web.resources.static-locations=classpath:/static/,file:%APP_HOME:/=/%/frontend/dist/ > "%LOG_DIR%\backend.log" 2>&1

call :wait_backend_up
if errorlevel 1 (
  echo [ERROR] Backend not reachable yet. Check logs\backend.log
  popd
  exit /b 1
)

echo [OK] Started. Open: http://127.0.0.1:8080/

popd
exit /b 0

rem --- subroutines ---

:wait_mysql_up
for /l %%i in (1,1,20) do (
  "%MYSQL_HOME%\bin\mysqladmin.exe" -uroot -proot ping >nul 2>nul
  if not errorlevel 1 exit /b 0
  ping 127.0.0.1 -n 2 >nul
)
exit /b 1

:wait_backend_up
for /l %%i in (1,1,30) do (
  powershell -NoProfile -Command "try{ $r=Invoke-WebRequest -Uri 'http://127.0.0.1:8080/index.html' -UseBasicParsing -TimeoutSec 2; if($r.StatusCode -ge 200){ exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>nul
  if not errorlevel 1 exit /b 0
  ping 127.0.0.1 -n 2 >nul
)
exit /b 1
