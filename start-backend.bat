@echo off
REM ASCII-only: avoids CMD misparsing UTF-8 inside this script.
chcp 65001 >nul
setlocal EnableExtensions
set "ROOT=%~dp0"

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] mvn not in PATH. Install Maven and add it to PATH.
    pause
    exit /b 1
)

if not exist "%ROOT%cashier-backend\pom.xml" (
    echo [ERROR] Missing backend: "%ROOT%cashier-backend\pom.xml"
    pause
    exit /b 1
)

echo [INFO] Starting Spring Boot in a new window: "%ROOT%cashier-backend"
start "Cashier-Backend" /D "%ROOT%cashier-backend" cmd /k call mvn spring-boot:run

endlocal
