@echo off
title "Cai dat Game vao Dien thoai Android qua USB"
cd /d "%~dp0.."

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
    echo [THONG BAO] Chua tim thay file APK. Dang tu dong bien dich...
    call scripts\build_apk.bat
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
    echo ===============================================================================
) else (
    echo.
    echo [CHU Y] Vui long kiem tra cap USB va bat USB Debugging tren dien thoai,
    echo hoac gui file 'build\UmlPatternsMasteryGame.apk' qua Zalo/Drive de cai truc tiep.
)

pause
