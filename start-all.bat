@echo off
REM ASCII-only: reliable under cmd.exe
chcp 65001 >nul
setlocal EnableExtensions
set "ROOT=%~dp0"

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] mvn not in PATH.
    pause
    exit /b 1
)

echo [INFO] Starting backend in a new window
start "Cashier-Backend" /D "%ROOT%cashier-backend" cmd /k call mvn spring-boot:run

if exist "%ROOT%cashier-frontend\package.json" (
    where npm >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] package.json found but npm not in PATH. Install Node.js.
        pause
        exit /b 1
    )
    echo [INFO] Starting frontend dev (source: cashier-frontend\src, http://localhost:5173 , /api -^> :8080)
    start "Cashier-Frontend" /D "%ROOT%cashier-frontend" cmd /k "if not exist node_modules\ (call npm install) & call npm run dev"
) else (
    echo [INFO] No cashier-frontend\package.json - skipped separate frontend.
    echo [INFO] With dist embedded by backend, open http://localhost:8080
)

endlocal
