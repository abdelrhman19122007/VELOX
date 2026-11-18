@echo off
rem Stops VELOX on this PC. The database and photos are kept; START-VELOX.bat brings everything back.
rem To wipe everything and start from zero:  docker compose --profile app down -v   (then delete .env and demo-data\local-accounts.txt)
cd /d "%~dp0"
docker compose --profile app stop
echo.
echo  VELOX stopped. Your data is kept.
pause
