@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo [VELOX] Java 17 or newer is required.
  pause
  exit /b 1
)

where python >nul 2>nul
if errorlevel 1 (
  echo [VELOX] Python 3 is required to serve the frontend.
  pause
  exit /b 1
)

echo [VELOX] Starting backend on http://localhost:8080 ...
start "VELOX Backend" cmd /k "cd /d ""%~dp0velox-backend"" && call mvnw.cmd spring-boot:run"

echo [VELOX] Starting frontend on http://localhost:63342 ...
start "VELOX Frontend" cmd /k "cd /d ""%~dp0velox-frontend"" && python -m http.server 63342 --bind 127.0.0.1"

timeout /t 8 /nobreak >nul
start "" "http://localhost:63342/login.html"

echo [VELOX] Started. Keep the Backend and Frontend windows open while using the app.
endlocal
