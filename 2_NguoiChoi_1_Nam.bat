@echo off
cd /d "%~dp0"
title "Multiplayer Client - Duy Nam"
echo ==================================================
echo   KET NOI PHONG DAU MANG: DUY NAM
echo   Host: 127.0.0.1:7777 | Snapshot Interpolation LERP
echo ==================================================
java -jar build\UmlPatternsMasteryGame.jar --client 127.0.0.1 7777 "Duy Nam"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Game Client ket thuc voi ma loi: %ERRORLEVEL%
)
pause
