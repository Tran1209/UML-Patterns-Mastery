@echo off
cd /d "%~dp0"
title "Dedicated Authoritative Game Server 20Hz (Headless)"
echo ==================================================
echo   KHOI DONG DEDICATED GAME SERVER 20Hz (HEADLESS)
echo   Cong lang nghe: 7777 | Che do: Authoritative Netcode
echo ==================================================
java -jar build\UmlPatternsMasteryGame.jar --server 7777
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Server ket thuc voi ma loi: %ERRORLEVEL%
)
pause
