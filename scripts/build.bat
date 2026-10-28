@echo off
title "Build and Package Game - UML Patterns Mastery"
cd /d "%~dp0.."

echo ==================================================
echo   DANG BIEN DICH VA DONG GOI GAME EXECUTABLE JAR...
echo ==================================================
if not exist "build\classes" mkdir "build\classes"

powershell -Command "Stop-Process -Name java, javaw -Force -ErrorAction SilentlyContinue; Start-Sleep -Milliseconds 200; javac -encoding UTF-8 -d build\classes (Get-ChildItem -Path Src_Game, Tests -Recurse -Filter *.java | ForEach-Object { $_.FullName })"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Bien dich that bai! Vui long kiem tra ma nguon Java.
    pause
    exit /b %ERRORLEVEL%
)

echo Main-Class: Main> build\manifest.txt
jar cfm build\UmlPatternsMasteryGame.jar build\manifest.txt -C build\classes .

echo ==================================================
echo   DONG GOI THANH CONG!
echo   File thuc thi: build\UmlPatternsMasteryGame.jar
echo ==================================================
pause
