@echo off
cd /d "%~dp0"
title QUAN LY DU AN - DUY NAM (LEAD ARCHITECT) - NHOM 1

set "MY_BRANCH=main"
set "MY_NAME=Duy Nam"
set "MY_EMAIL=oknha278@gmail.com"
set "MY_ROLE=Tran1209 - Lead Architect / Core"

:MENU
cls
echo ===============================================================================
echo          HE THONG QUAN LY DO AN OOP - 2D ACTION RPG ARENA (NHOM 1)
echo          Thanh vien : %MY_NAME% (%MY_ROLE%)
echo          Nhanh Git  : [%MY_BRANCH%] (Khong gian lam viec rieng cua %MY_NAME%)
echo ===============================================================================
echo   [1] Chay game tren PC (Bien dich va khoi dong ngay)
echo   [2] Bien dich lai ban Android (Build file APK cai vao dien thoai)
echo   [3] Day code local len nhanh [%MY_BRANCH%] tren GitHub
echo   [4] Keo code moi nhat tu [main] tren GitHub ve may tinh
echo   [0] Thoat
echo ===============================================================================
set "CHOICE="
set /p "CHOICE=Nhap lua chon cua ban (0-4): "
if not defined CHOICE exit /b 0
set "CHOICE=%CHOICE: =%"

if "%CHOICE%"=="1" goto RUN_PC
if "%CHOICE%"=="2" goto BUILD_APK
if "%CHOICE%"=="3" goto PUSH_BRANCH
if "%CHOICE%"=="4" goto SYNC_FROM_MAIN
if "%CHOICE%"=="0" exit /b 0
goto MENU

:RUN_PC
echo.
echo ===============================================================================
echo   DANG BIEN DICH CODE MOI NHAT VA KHOI DONG GAME PC 2.5D...
echo ===============================================================================
if not exist "build\classes" mkdir "build\classes"
powershell -Command "Stop-Process -Name java, javaw -Force -ErrorAction SilentlyContinue; Start-Sleep -Milliseconds 200; javac -encoding UTF-8 -d build\classes (Get-ChildItem -Path Src_Game, Tests -Recurse -Filter *.java | ForEach-Object { $_.FullName })"
if errorlevel 1 (
    echo [ERROR] Bien dich that bai! Vui long kiem tra lai ma nguon Java.
    pause
    goto MENU
)
echo Main-Class: Main> build\manifest.txt
jar cfm build\UmlPatternsMasteryGame.jar build\manifest.txt -C build\classes .
start "" javaw -jar build\UmlPatternsMasteryGame.jar
goto MENU

:BUILD_APK
echo.
if exist "scripts\build_apk.bat" (
    call scripts\build_apk.bat
) else (
    echo [ERROR] Khong tim thay scripts\build_apk.bat!
)
pause
goto MENU

:PUSH_BRANCH
echo.
echo ===============================================================================
echo   DAY CODE TU MAY LOCAL LEN NHANH [%MY_BRANCH%] TREN GITHUB
echo   Tac gia: %MY_NAME% ^<%MY_EMAIL%^>
echo ===============================================================================
git config user.name "%MY_NAME%"
git config user.email "%MY_EMAIL%"
echo.
set "COMMIT_MSG="
set /p "COMMIT_MSG=Nhap mo ta noi dung vua sua (Enter de dung mac dinh): "
if "%COMMIT_MSG%"=="" set "COMMIT_MSG=update(%MY_BRANCH%): cap nhat ma nguon va tai lieu boi %MY_NAME%"

echo.
echo [1/3] Dang kiem tra va gom tap tin thay doi (git add)...
git add -A
echo [2/3] Dang dong goi commit...
git commit -m "%COMMIT_MSG%"
echo [3/3] Dang day len GitHub nhanh [%MY_BRANCH%] (git push)...
git push -u origin %MY_BRANCH%
if errorlevel 1 (
    echo.
    echo [CANH BAO] Day len GitHub that bai! Co the can chay [4] de dong bo code truoc.
) else (
    echo.
    echo [THANH CONG] Code da duoc day len GitHub cua ban:
    echo   https://github.com/Tran1209/UML-Patterns-Mastery/tree/%MY_BRANCH%
)
pause
goto MENU

:SYNC_FROM_MAIN
echo.
echo ===============================================================================
echo   DONG BO CODE MOI NHAT CHO NHANH [%MY_BRANCH%]
echo ===============================================================================
echo [1/1] Dang tai va dong bo code tu origin/main ve may...
git pull origin main --no-edit
if errorlevel 1 (
    echo.
    echo [LOI] Khong the pull tu GitHub! Vui long kiem tra lai ket noi hoac conflict.
) else (
    echo.
    echo [THANH CONG] May local da dong bo hoan toan voi origin/main!
)
pause
goto MENU
