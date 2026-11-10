@echo off
echo ========================================================
echo    Starting ApexCare JavaFX Desktop Workstation
echo ========================================================

echo Launching Desktop Clinical Application...
call gradlew.bat :desktop:run
pause
