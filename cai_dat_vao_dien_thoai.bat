@echo off
title "Cai dat Game vao Dien thoai Android qua USB"
cd /d "%~dp0"

echo ===============================================================================
echo   DANG CAI DAT UML PATTERNS ARENA LEN DIEN THOAI ANDROID...
echo ===============================================================================

set "ADB=C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if not exist "%ADB%" (
    echo [ERROR] Khong tim thay ADB tai: %ADB%
    pause
    exit /b 1
)

if not exist "build\UmlPatternsMasteryGame.apk" (
    echo [THONG BAO] Chua tim thay file APK. Dang tu dong bien dich build_apk.bat...
    call build_apk.bat
)

echo Dang kiem tra ket noi thiet bi Android qua USB...
"%ADB%" devices

echo.
echo Dang cai dat APK: build\UmlPatternsMasteryGame.apk ...
"%ADB%" install -r build\UmlPatternsMasteryGame.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===============================================================================
    echo   [THANH CONG] Game da duoc cai dat truc tiep vao dien thoai!
    echo   Ban co the mo ung dung 'UML Patterns Arena' tren man hinh chinh de trai nghiem.
    echo ===============================================================================
) else (
    echo.
    echo [CHU Y] Neu chua thay game cai dat thanh cong, vui long kiem tra:
    echo 1. Da cam cap USB ket noi dien thoai voi may tinh chua?
    echo 2. Dien thoai da bat 'Tuy chon nha phat trien' (Developer Options) va 'Go loi USB' (USB Debugging) chua?
    echo 3. Tren man hinh dien thoai co hop thoai 'Cho phep go loi USB tu may tinh nay' khong? (Hay chon Cho phep).
    echo Hoac ban chi can copy file 'build\UmlPatternsMasteryGame.apk' vao dien thoai de cai truc tiep.
)

pause
