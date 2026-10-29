@echo off
chcp 65001 >nul
cd /d "%~dp0"
title "QUAN LY DU AN NHOM 5 - 2D ACTION RPG ARENA (HUCE OOP 2026)"

:MENU
cls
echo ===============================================================================
echo          HE THONG QUAN LY DO AN OOP - 2D ACTION RPG ARENA (NHOM 5)
echo ===============================================================================
echo   [1] Chay Game tren PC (Menu Sanh Cho / Choi Don / Tao Phong / Vao Phong)
echo   [2] Demo Nhanh Multiplayer tren PC (Tu mo 1 Server + 2 Client Nam ^& Kien)
echo   [3] Chay Bo Kiem Thu Tu Dong (15/15 Unit Tests - Clean Architecture)
echo   [4] Bien dich lai ban PC (Build Executable JAR)
echo   [5] Bien dich ban Android (Build Signed APK)
echo   [6] Cai dat APK truc tiep vao Dien thoai Android qua cap USB (ADB)
echo   [7] Dong bo Code len GitHub (Pull + Commit + Push tu dong)
echo   [0] Thoat
echo ===============================================================================
set /p CHOICE="Nhap lua chon cua ban (0-7): "

if "%CHOICE%"=="1" goto RUN_PC
if "%CHOICE%"=="2" goto DEMO_MULTI
if "%CHOICE%"=="3" goto RUN_TESTS
if "%CHOICE%"=="4" goto BUILD_JAR
if "%CHOICE%"=="5" goto BUILD_APK
if "%CHOICE%"=="6" goto INSTALL_APK
if "%CHOICE%"=="7" goto SYNC_GITHUB
if "%CHOICE%"=="0" exit /b 0
goto MENU

:RUN_PC
echo.
echo [DANG KHOI DONG GAME PC...]
start "" javaw -jar build\UmlPatternsMasteryGame.jar
goto MENU

:DEMO_MULTI
echo.
echo [DANG KHOI DONG SERVER 20HZ + 2 CUA SO NGUOI CHOI (NAM ^& KIEN)...]
start "Server 20Hz" cmd /c "java -jar build\UmlPatternsMasteryGame.jar --server 7777 & pause"
timeout /t 1 /nobreak >nul
start "Player 1 - Duy Nam" javaw -jar build\UmlPatternsMasteryGame.jar --client 127.0.0.1 7777 "Duy Nam"
start "Player 2 - Trung Kien" javaw -jar build\UmlPatternsMasteryGame.jar --client 127.0.0.1 7777 "Trung Kien"
goto MENU

:RUN_TESTS
echo.
java -jar build\UmlPatternsMasteryGame.jar --test
echo.
pause
goto MENU

:BUILD_JAR
echo.
call scripts\build.bat
goto MENU

:BUILD_APK
echo.
call scripts\build_apk.bat
goto MENU

:INSTALL_APK
echo.
call scripts\cai_dat_vao_dien_thoai.bat
goto MENU

:SYNC_GITHUB
echo.
echo ===============================================================================
echo   DONG BO DU AN LEN GITHUB (https://github.com/Tran1209/UML-Patterns-Mastery)
echo ===============================================================================
set /p COMMIT_MSG="Nhap noi dung cap nhat (Nhan Enter de dung mac dinh): "
if "%COMMIT_MSG%"=="" set "COMMIT_MSG=update: cap nhat ma nguon va tai lieu du an"
echo.
echo [1/3] Dang keo ban cap nhat moi nhat tu nhom ve (git pull)...
git pull origin main --no-edit
echo [2/3] Dang dong goi cac thay doi cuc bo (git add ^& commit)...
git add -A
git commit -m "%COMMIT_MSG%"
echo [3/3] Dang day code len GitHub (git push)...
git push -u origin main
echo.
echo ===============================================================================
echo   HOAN TAT DONG BO GITHUB!
echo   Link kho chua: https://github.com/Tran1209/UML-Patterns-Mastery
echo ===============================================================================
pause
goto MENU
