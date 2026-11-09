@echo off
echo ========================================================
echo    Building Hospital Management System (HMS)
echo ========================================================

if not exist bin mkdir bin
if not exist lib mkdir lib

if not exist "lib\sqlite-jdbc.jar" (
    echo [1/3] Downloading self-contained SQLite JDBC driver...
    curl -L "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.36.0.3/sqlite-jdbc-3.36.0.3.jar" -o "lib\sqlite-jdbc.jar"
) else (
    echo [1/3] SQLite JDBC driver found in lib\
)

echo [2/3] Finding all Java source files...
dir /s /B src\*.java > sources.txt

echo [3/3] Compiling with javac (Java 17)...
javac -cp "lib/*" -d bin @sources.txt
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    del sources.txt
    pause
    exit /b %errorlevel%
)

del sources.txt
echo [SUCCESS] Compilation completed successfully into bin/ folder!
