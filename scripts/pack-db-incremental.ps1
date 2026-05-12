# Database incremental only: one SQL + runner bat, offline-merge layout.
# Output: dist/supermarket-cashier-db-incremental.zip
# Run from repo root:  .\scripts\pack-db-incremental.ps1

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Path $PSScriptRoot -Parent
$OutDir = Join-Path $RepoRoot 'dist'
$Stage = Join-Path $OutDir 'db-incremental-staging'
$ZipPath = Join-Path $OutDir 'supermarket-cashier-db-incremental.zip'

$incSrc = Join-Path $RepoRoot 'cashier-backend\src\main\resources\db\incremental'
$runAllSql = Join-Path $incSrc 'run_all_incremental.sql'
if (-not (Test-Path $runAllSql)) { Write-Error "Missing: $runAllSql"; exit 1 }

New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
if (Test-Path $Stage) { Remove-Item $Stage -Recurse -Force }
New-Item -ItemType Directory -Path $Stage -Force | Out-Null

$dbInc = Join-Path $Stage 'db\incremental'
$dbsql = Join-Path $Stage 'dbsql'
New-Item -ItemType Directory -Path $dbInc -Force | Out-Null
New-Item -ItemType Directory -Path $dbsql -Force | Out-Null

Copy-Item -Path $runAllSql -Destination $dbInc -Force
Copy-Item -Path (Join-Path $incSrc 'README.md') -Destination $dbInc -ErrorAction SilentlyContinue
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\dbsql\run-incremental.bat') -Destination $dbsql -Force
Copy-Item -Path (Join-Path $RepoRoot 'scripts\offline-win64\dbsql\README.txt') -Destination $dbsql -Force

$howto = @(
    'Merge this zip into your offline root (same folder that contains backend, mysql, scripts).'
    ''
    '  db/incremental/     -> only run_all_incremental.sql (+ README.md). This is the single source for production upgrades.'
    '  dbsql/              -> run-incremental.bat (+ README.txt). The bat reads SQL from ..\\db\\incremental\\'
    ''
    'Cleanup (recommended once): under your offline tree, delete any OLD extra *.sql in dbsql\ and db\incremental'
    '(004, 005, post_offline, duplicate init snippets, etc.). Keep only what this zip provides.'
    ''
    'Then: start MySQL -> dbsql\run-incremental.bat'
    ''
    'Extract this zip to offline root (same level as backend, mysql, scripts).'
    'Recommended: remove leftover *.sql under dbsql and db\\incremental except files from this zip.'
) -join [Environment]::NewLine
Set-Content -Path (Join-Path $Stage 'MERGE-README.txt') -Value $howto -Encoding UTF8

$ZipTmp = Join-Path $OutDir 'db-incremental-pack.tmp.zip'
if (Test-Path $ZipTmp) { Remove-Item $ZipTmp -Force -ErrorAction SilentlyContinue }
Compress-Archive -Path (Join-Path $Stage '*') -DestinationPath $ZipTmp -CompressionLevel Optimal

$final = $ZipPath
if (Test-Path $ZipPath) {
    try { Remove-Item -LiteralPath $ZipPath -Force -ErrorAction Stop }
    catch {
        $final = Join-Path $OutDir ('supermarket-cashier-db-incremental-{0:yyyyMMdd-HHmmss}.zip' -f (Get-Date))
        Write-Host "NOTE: default zip locked; wrote: $final" -ForegroundColor Yellow
    }
}
Move-Item -LiteralPath $ZipTmp -Destination $final -Force
Write-Host "OK: $final" -ForegroundColor Green
