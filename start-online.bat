@echo off
setlocal
title ApexCare Hospital System - Live Public Deployment
echo ===================================================================
echo     ApexCare Hospital Management System - Live Online Deployment
echo     Powered by Cloudflare Global Edge Tunnel (Zero Credit Card)
echo ===================================================================
echo.

cd /d "%~dp0"

echo [1/2] Checking local Clinical Server on port 8088...
netstat -ano | findstr :8088 | findstr LISTENING >nul
if %errorlevel% equ 0 (
    echo [OK] Clinical Server is already active on port 8088!
) else (
    echo [..] Starting Clinical Server...
    if exist "server\build\libs\server-0.1.0.jar" (
        start "ApexCare Server" /min java -jar "server\build\libs\server-0.1.0.jar"
    ) else (
        start "ApexCare Server" /min cmd /c "call gradlew.bat :server:bootRun"
    )
    echo [..] Waiting for server to boot...
    timeout /t 6 /nobreak >nul
)

echo.
echo [2/2] Connecting to Cloudflare Global Edge...
echo ===================================================================
echo   Look for the line: "Your quick Tunnel has been created!"
echo   Copy the HTTPS link (e.g. https://xxxx.trycloudflare.com)
echo   Open it on your phone, laptop, or share with anyone worldwide!
echo ===================================================================
echo.

".tools\cloudflared.exe" tunnel --url http://localhost:8088
pause

