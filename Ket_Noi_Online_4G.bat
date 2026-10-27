@echo off
cd /d "%~dp0"
title "Ket Noi Dau Mang Online 4G / Internet"
echo ==================================================
echo   KET NOI DAU MANG MULTIPLAYER QUA 4G / INTERNET
echo ==================================================
set /p PNAME="Nhap ten cua ban (mac dinh: Hero): "
if "%PNAME%"=="" set PNAME=Hero

set /p PHOST="Nhap dia chi Server (vi du: 0.tcp.ap.ngrok.io hoac IP): "
if "%PHOST%"=="" set PHOST=127.0.0.1

set /p PPORT="Nhap cong Port (vi du: 12345 hoac 7777): "
if "%PPORT%"=="" set PPORT=7777

echo.
echo Dang ket noi toi %PHOST%:%PPORT% duoi ten %PNAME%...
java -jar build\UmlPatternsMasteryGame.jar --client %PHOST% %PPORT% "%PNAME%"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Game Client ket thuc voi ma loi: %ERRORLEVEL%
)
pause
