# 构建 Vue 前端并将 dist 复制到 cashier-backend/target/classes/static（Maven process-classes）
# 使用方法：在项目根目录执行  .\sync-frontend-to-backend.ps1  然后重启 Spring Boot

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Push-Location (Join-Path $root 'cashier-frontend')
if (-not (Test-Path 'node_modules')) {
  npm install
}
npm run build
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Push-Location (Join-Path $root 'cashier-backend')
mvn compile -q -DskipTests
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Write-Host 'OK: 静态资源已写入 cashier-backend/target/classes/static，请重启运行在 8080 的应用。' -ForegroundColor Green
