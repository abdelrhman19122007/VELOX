@echo off
rem ============================================================================
rem  VELOX - run everything on this PC with ONE double-click.
rem  Needs only Docker Desktop (running). No Java, no Flutter, no secrets from anyone.
rem  First run: about 5-10 minutes (downloads and builds). Next runs: under a minute.
rem  Guide: docs\TEAM_SETUP.md
rem ============================================================================
setlocal
cd /d "%~dp0"
title VELOX (local)

echo.
echo  [1/5] Checking Docker...
docker info >nul 2>&1
if errorlevel 1 (
  echo.
  echo  Docker is not running. Open "Docker Desktop", wait until it says "Engine running",
  echo  then double-click START-VELOX.bat again.
  echo.
  pause
  exit /b 1
)

echo  [2/5] Settings...
if not exist ".env" (
  powershell -NoProfile -Command ^
    "$r=[Security.Cryptography.RandomNumberGenerator]::Create(); function k($n){$b=New-Object byte[] $n; $r.GetBytes($b); ([Convert]::ToBase64String($b)) -replace '[^A-Za-z0-9]',''};" ^
    "$lines=@('# VELOX local settings - made by START-VELOX.bat, this PC only (git-ignored). Do not share.'," ^
    "'VELOX_ADMIN_EMAIL=admin@velox.local',('VELOX_ADMIN_PASSWORD=Admin-'+(k 18)),('VELOX_TOKEN_SECRET='+(k 48)),'VELOX_PORT=8080','VELOX_DB_PORT=55432');" ^
    "[IO.File]::WriteAllLines((Join-Path (Get-Location) '.env'), $lines)"
  echo        made .env with a random admin password and token key
) else (
  echo        using the existing .env
)

echo  [3/5] Starting database + server (first time builds the server, please wait)...
docker compose --profile app up -d --build
if errorlevel 1 (
  echo.
  echo  Docker could not start VELOX. Read the messages above, or see docs\TEAM_SETUP.md "Problems".
  pause
  exit /b 1
)

echo  [4/5] Waiting for VELOX to answer...
powershell -NoProfile -Command ^
  "$p=(Get-Content .env | ? {$_ -match '^VELOX_PORT='}) -replace 'VELOX_PORT=',''; if(-not $p){$p='8080'};" ^
  "for($i=0;$i -lt 100;$i++){ try{ if((Invoke-WebRequest -UseBasicParsing \"http://localhost:$p/api/zones\" -TimeoutSec 3).StatusCode -eq 200){ exit 0 } }catch{}; Start-Sleep 3 }; exit 1"
if errorlevel 1 (
  echo  VELOX did not answer in 5 minutes. See:  docker compose logs api
  pause
  exit /b 1
)

echo  [5/5] Loading the menus and demo accounts (first time only)...
docker compose --profile app wait demo-loader >nul 2>&1
docker compose --profile app logs --no-log-prefix demo-loader 2>&1 | findstr /v /c:"Waiting for"

powershell -NoProfile -Command ^
  "$p=(Get-Content .env | ? {$_ -match '^VELOX_PORT='}) -replace 'VELOX_PORT=',''; if(-not $p){$p='8080'};" ^
  "$ip=(Get-NetIPConfiguration -ErrorAction SilentlyContinue | ? { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } | Select-Object -First 1).IPv4Address.IPAddress;" ^
  "Write-Host ''; Write-Host '  VELOX is running:' -ForegroundColor Green;" ^
  "Write-Host \"    App (customer / store / driver)   http://localhost:$p/app/\" -ForegroundColor Yellow;" ^
  "Write-Host \"    Website                           http://localhost:$p/\" -ForegroundColor Yellow;" ^
  "Write-Host \"    Dashboard (admin / store)         http://localhost:$p/admin/\" -ForegroundColor Yellow;" ^
  "Write-Host \"    API docs (Swagger)                http://localhost:$p/swagger-ui.html\" -ForegroundColor Yellow;" ^
  "if($ip){ Write-Host \"    Phones on the same Wi-Fi          http://${ip}:$p/app/\" -ForegroundColor Cyan };" ^
  "Write-Host ''; Write-Host '  Logins: demo-data\local-accounts.txt  (admin password: VELOX_ADMIN_PASSWORD in .env)';" ^
  "Write-Host '  Stop: double-click STOP-VELOX.bat   (your data stays)'; Write-Host '';" ^
  "Start-Process \"http://localhost:$p/app/\""
echo.
pause
