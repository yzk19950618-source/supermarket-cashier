# Build app and zip only what you need to overlay an EXISTING offline tree.
# Output: dist/offline-incremental-patch.zip  (paths inside zip match offline layout)
# Run from repo root:  .\pack-offline-incremental.ps1

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$OutDir = Join-Path $RepoRoot 'dist'
$Stage = Join-Path $OutDir 'offline-incremental-staging'
$ZipPath = Join-Path $OutDir 'offline-incremental-patch.zip'

New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
if (Test-Path $Stage) { Remove-Item $Stage -Recurse -Force }
New-Item -ItemType Directory -Path $Stage -Force | Out-Null

Push-Location (Join-Path $RepoRoot 'cashier-frontend')
if (-not (Test-Path 'node_modules')) { npm install }
npm run build
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
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
Pop-Location

$backendDir = Join-Path $Stage 'backend'
$feDist = Join-Path $Stage 'frontend\dist'
$dbInc = Join-Path $Stage 'db\incremental'
New-Item -ItemType Directory -Path $backendDir -Force | Out-Null
New-Item -ItemType Directory -Path $feDist -Force | Out-Null
New-Item -ItemType Directory -Path $dbInc -Force | Out-Null

Copy-Item -Path $jar -Destination (Join-Path $backendDir 'cashier-backend.jar') -Force
Copy-Item -Path (Join-Path $RepoRoot 'cashier-frontend\dist\*') -Destination $feDist -Recurse -Force

$incSrc = Join-Path $RepoRoot 'cashier-backend\src\main\resources\db\incremental'
$runAllSql = Join-Path $incSrc 'run_all_incremental.sql'
if (-not (Test-Path $runAllSql)) { Write-Error "Missing: $runAllSql"; exit 1 }
Copy-Item -Path $runAllSql -Destination $dbInc -Force
Copy-Item -Path (Join-Path $incSrc 'README.md') -Destination $dbInc -ErrorAction SilentlyContinue

$dbsql = Join-Path $Stage 'dbsql'
New-Item -ItemType Directory -Path $dbsql -Force | Out-Null
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\dbsql\run-incremental.bat') -Destination $dbsql -Force
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\dbsql\README.txt') -Destination $dbsql -Force

$scriptsRuntime = Join-Path $Stage 'scripts\runtime'
New-Item -ItemType Directory -Path $scriptsRuntime -Force | Out-Null
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\runtime\start.bat') -Destination (Join-Path $scriptsRuntime 'start.bat') -Force
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\runtime\stop.bat') -Destination (Join-Path $scriptsRuntime 'stop.bat') -Force
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\runtime\db-local.bat.example') -Destination (Join-Path $scriptsRuntime 'db-local.bat.example') -Force

Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\PATCH-README.txt') -Destination (Join-Path $Stage 'PATCH-README.txt') -Force

$ZipTmp = Join-Path $OutDir 'offline-incremental-pack.tmp.zip'
if (Test-Path $ZipTmp) { Remove-Item $ZipTmp -Force -ErrorAction SilentlyContinue }
Compress-Archive -Path (Join-Path $Stage '*') -DestinationPath $ZipTmp -CompressionLevel Optimal

$final = $ZipPath
if (Test-Path $ZipPath) {
    try {
        Remove-Item -LiteralPath $ZipPath -Force -ErrorAction Stop
    } catch {
        $final = Join-Path $OutDir ('offline-incremental-patch-{0:yyyyMMdd-HHmmss}.zip' -f (Get-Date))
        Write-Host "NOTE: default zip is locked; wrote: $final" -ForegroundColor Yellow
    }
}
Move-Item -LiteralPath $ZipTmp -Destination $final -Force

Write-Host "OK: $final" -ForegroundColor Green
