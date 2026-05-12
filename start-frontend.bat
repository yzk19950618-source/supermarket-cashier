@echo off
REM ASCII-only: reliable under cmd.exe
chcp 65001 >nul
setlocal EnableExtensions
set "ROOT=%~dp0"
set "FE=%ROOT%cashier-frontend"

if not exist "%FE%\package.json" (
    echo [ERROR] Missing: "%FE%\package.json"
    pause
    exit /b 1
)

REM Dev server serves cashier-frontend/src (no dist required). Run "npm run build" when you need dist for Spring/static deploy.

where npm >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm not in PATH. Install Node.js 18+.
    pause
    exit /b 1
)

cd /d "%FE%"
if not exist node_modules\ (
    echo [INFO] Running npm install ...
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install failed.
        pause
        exit /b 1
    )
)

echo [INFO] Vite dev ^(源码 cashier-frontend/src^): http://localhost:5173
echo [INFO] Proxies /api and /uploads to http://localhost:8080
echo [INFO] Start backend first: start-backend.bat
start "Cashier-Frontend" /D "%FE%" cmd /k call npm run dev

endlocal
