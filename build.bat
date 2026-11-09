@echo off
echo ========================================================
echo    Building ApexCare Unified Hospital Management System
echo ========================================================

call gradlew.bat :server:classes :desktop:classes
if %errorlevel% neq 0 (
    echo [ERROR] Build failed.
    pause
    exit /b %errorlevel%
)
echo [SUCCESS] Unified build completed successfully!
