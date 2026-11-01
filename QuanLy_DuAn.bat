@echo off
cd /d "%~dp0"
title QUAN LY DU AN NHOM 5 - 2D ACTION RPG ARENA (HUCE OOP 2026)

:MENU
cls
set "CUR_BRANCH="
for /f "tokens=*" %%b in ('git branch --show-current 2^>nul') do set "CUR_BRANCH=%%b"
if "%CUR_BRANCH%"=="" for /f "tokens=*" %%b in ('git rev-parse --abbrev-ref HEAD 2^>nul') do set "CUR_BRANCH=%%b"
if "%CUR_BRANCH%"=="" set "CUR_BRANCH=main"

echo ===============================================================================
echo          HE THONG QUAN LY DO AN OOP - 2D ACTION RPG ARENA (NHOM 5)
echo          Nhanh Git hien tai: [%CUR_BRANCH%]
echo ===============================================================================
echo   [1] Chay Game tren PC (Menu Sanh Cho / Choi Don / Tao Phong / Vao Phong)
echo   [2] Demo Nhanh Multiplayer tren PC (Tu mo 1 Server + 2 Client Kien va Nam)
echo   [3] Chay Bo Kiem Thu Tu Dong (15/15 Unit Tests - Clean Architecture)
echo   [4] Bien dich lai ban PC (Build Executable JAR)
echo   [5] Bien dich ban Android (Build Signed APK)
echo   [6] Cai dat APK truc tiep vao Dien thoai Android qua cap USB (ADB)
echo   [7] Dong bo Code len GitHub (Tu dong Commit va Push len nhanh [%CUR_BRANCH%])
echo   [8] Quan ly Nhanh Lam Viec Nhom (Chuyen nhanh Kien / main / Gop code Merge)
echo   [0] Thoat
echo ===============================================================================
set "CHOICE="
set /p "CHOICE=Nhap lua chon cua ban (0-8): "
if not defined CHOICE exit /b 0
set "CHOICE=%CHOICE: =%"

if "%CHOICE%"=="1" goto RUN_PC
if "%CHOICE%"=="2" goto DEMO_MULTI
if "%CHOICE%"=="3" goto RUN_TESTS
if "%CHOICE%"=="4" goto BUILD_JAR
if "%CHOICE%"=="5" goto BUILD_APK
if "%CHOICE%"=="6" goto INSTALL_APK
if "%CHOICE%"=="7" goto SYNC_GITHUB
if "%CHOICE%"=="8" goto BRANCH_MENU
if "%CHOICE%"=="0" exit /b 0
goto MENU

:RUN_PC
echo.
echo [DANG BIEN DICH CODE MOI NHAT VA KHOI DONG GAME 2.5D PC...]
if not exist "build\classes" mkdir "build\classes"
powershell -Command "Stop-Process -Name java, javaw -Force -ErrorAction SilentlyContinue; Start-Sleep -Milliseconds 200; javac -encoding UTF-8 -d build\classes (Get-ChildItem -Path Src_Game, Tests -Recurse -Filter *.java | ForEach-Object { $_.FullName })"
echo Main-Class: Main> build\manifest.txt
jar cfm build\UmlPatternsMasteryGame.jar build\manifest.txt -C build\classes .
start "" javaw -jar build\UmlPatternsMasteryGame.jar
goto MENU

:DEMO_MULTI
echo.
echo [DANG BIEN DICH VA KHOI DONG SERVER 20HZ + 2 CLIENT (KIEN VA NAM)...]
if not exist "build\classes" mkdir "build\classes"
powershell -Command "Stop-Process -Name java, javaw -Force -ErrorAction SilentlyContinue; Start-Sleep -Milliseconds 200; javac -encoding UTF-8 -d build\classes (Get-ChildItem -Path Src_Game, Tests -Recurse -Filter *.java | ForEach-Object { $_.FullName })"
echo Main-Class: Main> build\manifest.txt
jar cfm build\UmlPatternsMasteryGame.jar build\manifest.txt -C build\classes .
start "Server 20Hz" cmd /c "java -jar build\UmlPatternsMasteryGame.jar --server 7777 & pause"
timeout /t 1 /nobreak >nul
start "Player 1 - Trung Kien" javaw -jar build\UmlPatternsMasteryGame.jar --client 127.0.0.1 7777 "Trung Kien"
start "Player 2 - Duy Nam" javaw -jar build\UmlPatternsMasteryGame.jar --client 127.0.0.1 7777 "Duy Nam"
goto MENU

:RUN_TESTS
echo.
if not exist "build\classes" mkdir "build\classes"
powershell -Command "javac -encoding UTF-8 -d build\classes (Get-ChildItem -Path Src_Game, Tests -Recurse -Filter *.java | ForEach-Object { $_.FullName })"
echo Main-Class: Main> build\manifest.txt
jar cfm build\UmlPatternsMasteryGame.jar build\manifest.txt -C build\classes .
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
echo   DONG BO DU AN LEN NHANH [%CUR_BRANCH%] TREN GITHUB
echo ===============================================================================
set "COMMIT_MSG="
set /p "COMMIT_MSG=Nhap noi dung cap nhat (Nhan Enter de dung mac dinh): "
if "%COMMIT_MSG%"=="" set "COMMIT_MSG=update(%CUR_BRANCH%): cap nhat ma nguon va tai lieu du an"
echo.
echo [1/3] Dang cap nhat du lieu moi nhat tu GitHub (git fetch va pull)...
git fetch --all
git pull origin %CUR_BRANCH% --no-edit
echo [2/3] Dang dong goi cac thay doi cuc bo (git add va commit)...
git add -A
git commit -m "%COMMIT_MSG%"
echo [3/3] Dang day code len nhanh [%CUR_BRANCH%] (git push)...
git push -u origin %CUR_BRANCH%
echo.
echo ===============================================================================
echo   HOAN TAT! Code da duoc day len nhanh [%CUR_BRANCH%] tren GitHub!
echo   Link kho chua: https://github.com/Tran1209/UML-Patterns-Mastery/tree/%CUR_BRANCH%
echo ===============================================================================
pause
goto MENU

:BRANCH_MENU
cls
echo ===============================================================================
echo          QUAN LY NHANH LAM VIEC NHOM (GIT BRANCH WORKFLOW)
echo          Nhanh hien tai cua may nay: [%CUR_BRANCH%]
echo ===============================================================================
echo   --- CHUYEN NHANH LAM VIEC ---
echo   [1] Chuyen sang nhanh [Kien]  (Nhom truong Kien)
echo   [2] Chuyen sang nhanh [Huy]   (Duc Huy)
echo   [3] Chuyen sang nhanh [Anh]   (Duc Anh)
echo   [4] Chuyen sang nhanh [Hoang] (Huy Hoang)
echo   [5] Chuyen ve nhanh chinh [main]
echo   --- GOP CODE VAO NHANH CHINH (MERGE) ---
echo   [6] Gop code tu nhanh [Kien]  vao [main]
echo   [7] Gop code tu nhanh [Huy]   vao [main]
echo   [8] Gop code tu nhanh [Anh]   vao [main]
echo   [9] Gop code tu nhanh [Hoang] vao [main]
echo   --- DONG BO CODE MOI NHAT TU [main] SANG NHANH CON ---
echo   [10] Dong bo tu [main] sang [Kien]
echo   [11] Dong bo tu [main] sang [Huy]
echo   [12] Dong bo tu [main] sang [Anh]
echo   [13] Dong bo tu [main] sang [Hoang]
echo   [0] Quay lai Menu chinh
echo ===============================================================================
set "BCHOICE="
set /p "BCHOICE=Chon thao tac nhanh (0-13): "
if not defined BCHOICE goto MENU
set "BCHOICE=%BCHOICE: =%"
if "%BCHOICE%"=="1" goto BR_KIEN
if "%BCHOICE%"=="2" goto BR_HUY
if "%BCHOICE%"=="3" goto BR_ANH
if "%BCHOICE%"=="4" goto BR_HOANG
if "%BCHOICE%"=="5" goto BR_MAIN
if "%BCHOICE%"=="6" goto BR_MERGE_KIEN
if "%BCHOICE%"=="7" goto BR_MERGE_HUY
if "%BCHOICE%"=="8" goto BR_MERGE_ANH
if "%BCHOICE%"=="9" goto BR_MERGE_HOANG
if "%BCHOICE%"=="10" goto BR_SYNC_KIEN
if "%BCHOICE%"=="11" goto BR_SYNC_HUY
if "%BCHOICE%"=="12" goto BR_SYNC_ANH
if "%BCHOICE%"=="13" goto BR_SYNC_HOANG
if "%BCHOICE%"=="0" goto MENU
goto MENU

:BR_KIEN
git fetch --all
git checkout Kien || git checkout -b Kien origin/Kien
if errorlevel 1 (echo [THAT BAI] Khong the chuyen sang nhanh [Kien]! & pause & goto MENU)
git pull origin Kien --no-edit
echo [THANH CONG] Da chuyen sang nhanh [Kien]!
pause
goto MENU

:BR_HUY
git fetch --all
git checkout Huy || git checkout -b Huy origin/Huy
if errorlevel 1 (echo [THAT BAI] Khong the chuyen sang nhanh [Huy]! & pause & goto MENU)
git pull origin Huy --no-edit
echo [THANH CONG] Da chuyen sang nhanh [Huy]!
pause
goto MENU

:BR_ANH
git fetch --all
git checkout Anh || git checkout -b Anh origin/Anh
if errorlevel 1 (echo [THAT BAI] Khong the chuyen sang nhanh [Anh]! & pause & goto MENU)
git pull origin Anh --no-edit
echo [THANH CONG] Da chuyen sang nhanh [Anh]!
pause
goto MENU

:BR_HOANG
git fetch --all
git checkout Hoang || git checkout -b Hoang origin/Hoang
if errorlevel 1 (echo [THAT BAI] Khong the chuyen sang nhanh [Hoang]! & pause & goto MENU)
git pull origin Hoang --no-edit
echo [THANH CONG] Da chuyen sang nhanh [Hoang]!
pause
goto MENU

:BR_MAIN
git fetch --all
git checkout main
git pull origin main --no-edit
echo [THANH CONG] Da chuyen ve nhanh chinh [main]!
pause
goto MENU

:BR_MERGE_KIEN
echo Dang keo code moi nhat tu nhanh [Kien] gop vao [main]...
git fetch --all
git checkout main
git pull origin main --no-edit
git merge origin/Kien -m "merge: gop nhanh Kien vao main"
git push origin main
echo [THANH CONG] Da gop toan bo thay doi cua nhanh [Kien] vao [main]!
git checkout Kien
pause
goto MENU

:BR_MERGE_HUY
echo Dang keo code moi nhat tu nhanh [Huy] gop vao [main]...
git fetch --all
git checkout main
git pull origin main --no-edit
git merge origin/Huy -m "merge: gop nhanh Huy vao main"
git push origin main
echo [THANH CONG] Da gop toan bo thay doi cua nhanh [Huy] vao [main]!
git checkout Huy
pause
goto MENU

:BR_MERGE_ANH
echo Dang keo code moi nhat tu nhanh [Anh] gop vao [main]...
git fetch --all
git checkout main
git pull origin main --no-edit
git merge origin/Anh -m "merge: gop nhanh Anh vao main"
git push origin main
echo [THANH CONG] Da gop toan bo thay doi cua nhanh [Anh] vao [main]!
git checkout Anh
pause
goto MENU

:BR_MERGE_HOANG
echo Dang keo code moi nhat tu nhanh [Hoang] gop vao [main]...
git fetch --all
git checkout main
git pull origin main --no-edit
git merge origin/Hoang -m "merge: gop nhanh Hoang vao main"
git push origin main
echo [THANH CONG] Da gop toan bo thay doi cua nhanh [Hoang] vao [main]!
git checkout Hoang
pause
goto MENU

:BR_SYNC_KIEN
echo Dang dong bo code moi nhat tu [main] sang [Kien]...
git fetch --all
git checkout Kien || git checkout -b Kien origin/Kien
if errorlevel 1 (echo [THAT BAI] Khong the chuyen sang nhanh [Kien]! & pause & goto MENU)
git merge origin/main -m "merge: cap nhat main vao nhanh Kien"
git push origin Kien
echo [THANH CONG] Nhanh [Kien] da co code moi nhat tu [main]!
pause
goto MENU

:BR_SYNC_HUY
echo Dang dong bo code moi nhat tu [main] sang [Huy]...
git fetch --all
git checkout Huy || git checkout -b Huy origin/Huy
if errorlevel 1 (echo [THAT BAI] Khong the chuyen sang nhanh [Huy]! & pause & goto MENU)
git merge origin/main -m "merge: cap nhat main vao nhanh Huy"
git push origin Huy
echo [THANH CONG] Nhanh [Huy] da co code moi nhat tu [main]!
pause
goto MENU

:BR_SYNC_ANH
echo Dang dong bo code moi nhat tu [main] sang [Anh]...
git fetch --all
git checkout Anh || git checkout -b Anh origin/Anh
if errorlevel 1 (echo [THAT BAI] Khong the chuyen sang nhanh [Anh]! & pause & goto MENU)
git merge origin/main -m "merge: cap nhat main vao nhanh Anh"
git push origin Anh
echo [THANH CONG] Nhanh [Anh] da co code moi nhat tu [main]!
pause
goto MENU

:BR_SYNC_HOANG
echo Dang dong bo code moi nhat tu [main] sang [Hoang]...
git fetch --all
git checkout Hoang || git checkout -b Hoang origin/Hoang
if errorlevel 1 (echo [THAT BAI] Khong the chuyen sang nhanh [Hoang]! & pause & goto MENU)
git merge origin/main -m "merge: cap nhat main vao nhanh Hoang"
git push origin Hoang
echo [THANH CONG] Nhanh [Hoang] da co code moi nhat tu [main]!
pause
goto MENU
