@echo off
chcp 65001 >nul
echo ============================================================================
echo   MO 2 CUA SO SURVIVAL HEROES DE TEST MOI PHONG VA CHOI CHUNG (CO-OP/PVP)
echo ============================================================================
echo   Huong dan:
echo   1. O Cua so 1: Nhap Photon AppId -> Bam [Vao Phong] -> Bam [COPY MOI]
echo   2. O Cua so 2: Doi Ten Nhan Vat -> Bam [DAN & VAO]
echo   3. Ca 2 se gap nhau truc tiep trong cung Phong [SH-8899]!
echo ============================================================================
start "Player 1 - Survival Heroes" "C:\Users\nam\Downloads\Godot_v4.7.2-stable_mono_win64\Godot_v4.7.2-stable_mono_win64\Godot_v4.7.2-stable_mono_win64.exe" --path "E:\duan1\new-game-project" --position 20,40
timeout /t 2 >nul
start "Player 2 - Survival Heroes" "C:\Users\nam\Downloads\Godot_v4.7.2-stable_mono_win64\Godot_v4.7.2-stable_mono_win64\Godot_v4.7.2-stable_mono_win64.exe" --path "E:\duan1\new-game-project" --position 480,80
