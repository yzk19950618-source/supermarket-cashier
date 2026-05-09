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

if not exist "%FE%\dist\index.html" (
    echo [ERROR] Missing: "%FE%\dist\index.html"
    echo [INFO] Build the SPA into dist first, or open http://localhost:8080 via start-backend.bat
    pause
    exit /b 1
)

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

echo [INFO] Vite preview: http://localhost:5173  (proxies /api to http://localhost:8080)
echo [INFO] Start backend first: start-backend.bat
start "Cashier-Frontend" /D "%FE%" cmd /k call npm run dev

endlocal
