@echo off
echo ========================================================
echo    Starting ApexCare Unified Clinical Server
echo    (Shared Database for Desktop, Android & Web Portal)
echo ========================================================

echo Server starting on http://localhost:8088/ ...
call gradlew.bat :server:bootRun
pause
