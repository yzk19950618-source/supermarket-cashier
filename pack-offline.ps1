# Build and copy into offline layout (default: D:\Users\PC\supermarket-cashier_v1.0.1_win64_offline)
# Run from repo root:  .\pack-offline.ps1  [-OfflineRoot "D:\path\to\offline"]

param(
    [string] $OfflineRoot = 'D:\Users\PC\supermarket-cashier_v1.0.1_win64_offline'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not (Test-Path $OfflineRoot)) {
    Write-Error "Offline root not found: $OfflineRoot"
}

Push-Location (Join-Path $RepoRoot 'cashier-frontend')
if (-not (Test-Path 'node_modules')) { npm install }
npm run build
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
$dist = Join-Path $RepoRoot 'cashier-frontend\dist'
$feTarget = Join-Path $OfflineRoot 'frontend\dist'
if (Test-Path $feTarget) { Remove-Item $feTarget -Recurse -Force }
New-Item -ItemType Directory -Path $feTarget -Force | Out-Null
Copy-Item -Path (Join-Path $dist '*') -Destination $feTarget -Recurse -Force
Pop-Location

Push-Location (Join-Path $RepoRoot 'cashier-frontend')
npm run sync-static
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Push-Location (Join-Path $RepoRoot 'cashier-backend')
mvn -q package -DskipTests
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
$jar = Join-Path $RepoRoot 'cashier-backend\target\cashier-backend-1.0.0.jar'
if (-not (Test-Path $jar)) { Pop-Location; Write-Error "Missing jar: $jar"; exit 1 }
$jarDest = Join-Path $OfflineRoot 'backend\cashier-backend.jar'
Copy-Item -Path $jar -Destination $jarDest -Force
Pop-Location

$dbRoot = Join-Path $OfflineRoot 'db'
$incSrc = Join-Path $RepoRoot 'cashier-backend\src\main\resources\db\incremental'
$incDest = Join-Path $dbRoot 'incremental'
$runAllSql = Join-Path $incSrc 'run_all_incremental.sql'
if (-not (Test-Path $runAllSql)) { Write-Error "Missing: $runAllSql"; exit 1 }
$pInit = Join-Path $dbRoot 'init.sql'
$pRegion = Join-Path $dbRoot 'region-init.sql'
if (Test-Path $pInit) { Remove-Item $pInit -Force }
if (Test-Path $pRegion) { Remove-Item $pRegion -Force }
if (Test-Path $incDest) { Remove-Item $incDest -Recurse -Force }
New-Item -ItemType Directory -Path $incDest -Force | Out-Null
Copy-Item -Path $runAllSql -Destination $incDest -Force
Copy-Item -Path (Join-Path $RepoRoot 'cashier-backend\src\main\resources\db\incremental\README.md') -Destination $incDest -ErrorAction SilentlyContinue
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\db-README.txt') -Destination (Join-Path $dbRoot 'README.txt') -Force

$dbsqlDest = Join-Path $OfflineRoot 'dbsql'
if (Test-Path $dbsqlDest) { Remove-Item $dbsqlDest -Recurse -Force }
New-Item -ItemType Directory -Path $dbsqlDest -Force | Out-Null
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\dbsql\run-incremental.bat') -Destination $dbsqlDest -Force
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\dbsql\README.txt') -Destination $dbsqlDest -Force

Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\runtime\start.bat') -Destination (Join-Path $OfflineRoot 'scripts\runtime\start.bat') -Force
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\runtime\stop.bat') -Destination (Join-Path $OfflineRoot 'scripts\runtime\stop.bat') -Force
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\runtime\db-local.bat.example') -Destination (Join-Path $OfflineRoot 'scripts\runtime\db-local.bat.example') -Force

Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\init-db.bat') -Destination (Join-Path $OfflineRoot 'scripts\init\init-db.bat') -Force
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\init-region.bat') -Destination (Join-Path $OfflineRoot 'scripts\init\init-region.bat') -Force
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\README-offline.txt') -Destination (Join-Path $OfflineRoot 'README-offline.txt') -Force

Write-Host "OK: Pack written to $OfflineRoot" -ForegroundColor Green
