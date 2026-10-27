@echo off
title "Build and Package Game - UML Patterns Mastery"
echo ==================================================
echo   DANG BIEN DICH VA DONG GOI GAME EXECUTABLE JAR...
echo ==================================================
if not exist "bin" mkdir bin
if not exist "build" mkdir build

powershell -Command "javac -d bin (Get-ChildItem -Path Src_Game, Tests -Recurse -Filter *.java | ForEach-Object { $_.FullName })"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Bien dich that bai! Vui long kiem tra ma nguon Java.
    pause
    exit /b %ERRORLEVEL%
)

echo Main-Class: Main> build\manifest.txt
jar cfm build\UmlPatternsMasteryGame.jar build\manifest.txt -C bin .

echo ==================================================
echo   DONG GOI THANH CONG!
echo   File thuc thi: build/UmlPatternsMasteryGame.jar
echo ==================================================
pause
