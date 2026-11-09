@echo off
echo ========================================================
echo    Starting Hospital Management System Web Portal
echo ========================================================

if not exist bin (
    echo [INFO] bin folder not found. Building project first...
    call build.bat
)

echo Starting Java HTTP Server on http://localhost:8088 ...
java -cp "bin;lib/*" com.hospital.Main 8088
pause
