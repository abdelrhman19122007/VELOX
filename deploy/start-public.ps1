# Puts VELOX v2 on the internet from this PC with a free Cloudflare "quick tunnel" (no account, no card).
#   /         website            /admin/   admin and store dashboard
#   /app/     the app in a browser          /downloads/velox.apk   the Android app
# The link changes every run; the phone app finds the new one by itself (ntfy.sh topic below).
# Keep this window open; close it (or Ctrl+C) to go offline.
#
# Needs: Docker Desktop, Java 17, cloudflared in %USERPROFILE%\cloudflared, Flutter (first build only).
# First run asks for the admin's email and password and keeps them, with a random token secret, in deploy\.env
# (git-ignored — never commit it).

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $PSScriptRoot '.env'
$cloudflared = Join-Path $env:USERPROFILE 'cloudflared\cloudflared.exe'
$logs = Join-Path $root 'deploy\logs'
New-Item -ItemType Directory -Force $logs | Out-Null
Set-Location $root

# ---------- settings (deploy\.env) ----------
if (-not (Test-Path $envFile)) {
    Write-Host 'First run: the admin account' -ForegroundColor Cyan
    $email = Read-Host 'Admin email'
    $pass = Read-Host 'Admin password (12+ characters)'
    if ($pass.Length -lt 12) { throw 'Use a password of at least 12 characters.' }
    $bytes = New-Object byte[] 48
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    @(
        "VELOX_ADMIN_EMAIL=$email",
        "VELOX_ADMIN_PASSWORD=$pass",
        "VELOX_TOKEN_SECRET=$([Convert]::ToBase64String($bytes))",
        'VELOX_DB_PORT=5434',
        'VELOX_DB_NAME=velox_live',
        'VELOX_API_DOCS=false'
    ) | Set-Content -Encoding utf8 $envFile
}
Get-Content $envFile | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item "env:$k" $v }
$port = 8080
$env:PORT = "$port"
$env:DB_URL = "jdbc:postgresql://localhost:$($env:VELOX_DB_PORT)/$($env:VELOX_DB_NAME)"
$env:DB_USER = 'velox'
$env:DB_PASSWORD = 'velox'

function Test-Server {
    try { (Invoke-WebRequest -UseBasicParsing "http://localhost:$port/api/zones" -TimeoutSec 3).StatusCode -eq 200 } catch { $false }
}

# ---------- 1. database ----------
Write-Host '1/5  Database...' -ForegroundColor Cyan
docker compose up -d db | Out-Null
for ($i = 0; $i -lt 30; $i++) { docker exec velox-db pg_isready -U velox *> $null; if ($LASTEXITCODE -eq 0) { break }; Start-Sleep 2 }
$exists = docker exec velox-db psql -U velox -tAc "SELECT 1 FROM pg_database WHERE datname='$($env:VELOX_DB_NAME)'"
if (-not $exists) { docker exec velox-db createdb -U velox $env:VELOX_DB_NAME | Out-Null }

# ---------- 2. app for the browser + APK for download ----------
$web = Join-Path $root 'mobile\build\web\index.html'
if (-not (Test-Path $web)) {
    Write-Host '2/5  Building the app for the browser (first time, ~1 min)...' -ForegroundColor Cyan
    Push-Location (Join-Path $root 'mobile'); flutter build web --release --base-href /app/ | Out-Null; Pop-Location
} else { Write-Host '2/5  Browser app already built' -ForegroundColor Cyan }
$apk = Join-Path $root 'mobile\build\app\outputs\flutter-apk\app-release.apk'
if (Test-Path $apk) {
    New-Item -ItemType Directory -Force (Join-Path $root 'web\downloads') | Out-Null
    Copy-Item $apk (Join-Path $root 'web\downloads\velox.apk') -Force
}

# ---------- 3. server ----------
if (-not (Test-Server)) {
    Write-Host '3/5  Starting the server...' -ForegroundColor Cyan
    $jar = Get-ChildItem (Join-Path $root 'backend\target') -Filter 'velox-backend-*.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jar) {
        Push-Location (Join-Path $root 'backend'); .\mvnw.cmd -q -DskipTests package; Pop-Location
        $jar = Get-ChildItem (Join-Path $root 'backend\target') -Filter 'velox-backend-*.jar' | Select-Object -First 1
    }
    Start-Process -WindowStyle Minimized -WorkingDirectory (Join-Path $root 'backend') -FilePath 'java' `
        -ArgumentList '-jar', $jar.FullName -RedirectStandardOutput "$logs\server.log" -RedirectStandardError "$logs\server.err"
    for ($i = 0; $i -lt 60 -and -not (Test-Server); $i++) { Start-Sleep 3 }
    if (-not (Test-Server)) { throw "The server didn't start — see $logs\server.log" }
} else { Write-Host '3/5  Server already running' -ForegroundColor Cyan }

# ---------- 4. tunnel (restarted when the internet drops) ----------
Write-Host '4/5  Opening the tunnel...' -ForegroundColor Cyan
$topic = 'velox-nabeeh-k7q2m9'   # same as discoveryTopic in mobile/lib/core/api.dart; only the public link is posted
Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force
$first = $true
while ($true) {
    $log = "$logs\tunnel-$(Get-Date -Format 'yyyyMMdd-HHmmss').log"
    $tunnel = Start-Process -PassThru -WindowStyle Hidden -FilePath $cloudflared `
        -ArgumentList 'tunnel', '--no-autoupdate', '--url', "http://localhost:$port" -RedirectStandardError $log
    $url = $null
    for ($i = 0; $i -lt 40 -and -not $url -and -not $tunnel.HasExited; $i++) {
        Start-Sleep 1
        if (Test-Path $log) {
            $m = Select-String -Path $log -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' | Select-Object -First 1
            if ($m) { $url = $m.Matches[0].Value }
        }
    }
    if (-not $url) { Write-Host '  No link yet (internet?) — retrying in 10 s' -ForegroundColor Red; Stop-Process -Id $tunnel.Id -ErrorAction SilentlyContinue; Start-Sleep 10; continue }
    try { Invoke-RestMethod -Method Post -Uri "https://ntfy.sh/$topic" -Body $url -TimeoutSec 15 | Out-Null } catch {
        Write-Host '  (could not announce the link — type it in the app: Settings → Server link)' -ForegroundColor DarkYellow
    }
    Write-Host ''
    Write-Host "5/5  VELOX is online ($(Get-Date -Format 'HH:mm')):" -ForegroundColor Green
    Write-Host "    Website    $url/" -ForegroundColor Yellow
    Write-Host "    Dashboard  $url/admin/" -ForegroundColor Yellow
    Write-Host "    App        $url/app/" -ForegroundColor Yellow
    if (Test-Path (Join-Path $root 'web\downloads\velox.apk')) { Write-Host "    Android    $url/downloads/velox.apk" -ForegroundColor Yellow }
    if ($first) { Write-Host ''; Write-Host '  Keep this window open. Close it to go offline.' -ForegroundColor Gray; $first = $false }
    Set-Clipboard "$url/"
    Wait-Process -Id $tunnel.Id
    Write-Host "  Tunnel dropped at $(Get-Date -Format 'HH:mm') — reconnecting..." -ForegroundColor Red
    Start-Sleep 3
}
