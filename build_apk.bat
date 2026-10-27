@echo off
title "Build Android APK - UML Patterns Mastery"
cd /d "%~dp0"

echo ===============================================================================
echo   DANG BIEN DICH VA DONG GOI ANDROID APK (HUCE OOP ARENA)...
echo ===============================================================================

set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.2.1"
set "SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk"
if not exist "%SDK_ROOT%" (
    if defined ANDROID_HOME set "SDK_ROOT=%ANDROID_HOME%"
)

set "BT_DIR=%SDK_ROOT%\build-tools\36.0.0"
if not exist "%BT_DIR%" (
    set "BT_DIR=%SDK_ROOT%\build-tools\34.0.0"
)

set "ANDROID_JAR=%SDK_ROOT%\platforms\android-34\android.jar"
set "AAPT2=%BT_DIR%\aapt2.exe"
set "D8=%BT_DIR%\d8.bat"
set "ZIPALIGN=%BT_DIR%\zipalign.exe"
set "APKSIGNER=%BT_DIR%\apksigner.bat"

if not exist "%ANDROID_JAR%" (
    echo [ERROR] Khong tim thay android.jar tai %ANDROID_JAR%!
    pause
    exit /b 1
)

echo [1/5] Tao cac thu muc build can thiet...
if not exist "build" mkdir "build"
if not exist "Android_Game\build\classes" mkdir "Android_Game\build\classes"
if not exist "Android_Game\gen" mkdir "Android_Game\gen"

echo [2/5] Bien dich tai nguyen Android voi AAPT2...
"%AAPT2%" compile --dir Android_Game\res -o Android_Game\build\res.zip
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] AAPT2 compile res that bai!
    pause
    exit /b %ERRORLEVEL%
)

"%AAPT2%" link -I "%ANDROID_JAR%" --manifest Android_Game\AndroidManifest.xml -o Android_Game\build\base.apk --java Android_Game\gen Android_Game\build\res.zip --auto-add-overlay
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] AAPT2 link that bai!
    pause
    exit /b %ERRORLEVEL%
)

echo [3/5] Bien dich ma nguon Java (Domain Core + Android UI)...
powershell -Command "javac --release 17 -cp '%ANDROID_JAR%;Android_Game/build/classes' -d Android_Game/build/classes (Get-ChildItem -Path Src_Game/Core/*.java, Src_Game/Combat/*.java, Src_Game/Network/*.java, Src_Game/Render/Direction.java, Src_Game/Render/LpcAnimationType.java, Src_Game/Render/LayerType.java, Src_Game/Render/FrameBounds.java, Src_Game/Render/Camera2D.java, Src_Game/Render/CharacterRenderer.java, Src_Game/Render/CompositeCharacterVisual.java, Src_Game/Render/SpriteLayer.java, Src_Game/Render/LpcSpriteSheet.java, Src_Game/Render/LpcCharacterBuilder.java, Src_Game/UI/IEntityEventListener.java, Src_Game/UI/HudObserver.java, Android_Game/gen/com/huce/oop/game/R.java, Android_Game/src/com/huce/oop/game/*.java | ForEach-Object { $_.FullName })"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] javac that bai!
    pause
    exit /b %ERRORLEVEL%
)

echo [4/5] Chuyen doi Bytecode sang Dalvik Executable (classes.dex) voi D8...
powershell -Command "$classes = (Get-ChildItem -Path Android_Game/build/classes -Recurse -Filter *.class | ForEach-Object { $_.FullName }); & '%D8%' --min-api 21 --lib '%ANDROID_JAR%' --output Android_Game/build $classes"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] D8 dex conversion that bai!
    pause
    exit /b %ERRORLEVEL%
)

echo [5/5] Dong goi APK, Zipalign 4-byte va Ky chung chi so (Apksigner)...
jar -uf Android_Game\build\base.apk -C Android_Game\build classes.dex
"%ZIPALIGN%" -f -p 4 Android_Game\build\base.apk Android_Game\build\aligned.apk
"%APKSIGNER%" sign --ks Android_Game\debug.keystore --ks-pass pass:android --key-pass pass:android --out build\UmlPatternsMasteryGame.apk Android_Game\build\aligned.apk
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Ky APK that bai!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===============================================================================
echo   BIEN DICH VA DONG GOI APK THANH CONG 100%!
echo   File APK da san sang tai: build\UmlPatternsMasteryGame.apk
echo ===============================================================================
echo   HUONG DAN CAI DAT LEN DIEN THOAI ANDROID:
echo   1. Cam cap USB hoac gui file 'build\UmlPatternsMasteryGame.apk' qua Zalo/Drive.
echo   2. Tren dien thoai, an vao file APK va chon 'Cai dat' (Install).
echo   3. Neu may hoi 'Cai dat tu nguon khong xac dinh', hay chon 'Cho phep' (Allow).
echo   4. Neu cam dien thoai bat che do USB Debugging, co the chay lenh:
echo      adb install -r build\UmlPatternsMasteryGame.apk
echo ===============================================================================
pause
