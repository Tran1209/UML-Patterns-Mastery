@echo off
title "Mo Cong Online Ra Internet (Khong Can Cai Dat, Khong Can Token)"
echo ==================================================
echo   DANG MO CONG 7777 RA INTERNET TOAN CAU QUA SSH...
echo   (Khong can tai phan mem gi them, khong can dang ky token)
echo ==================================================
echo.
echo Khi ket noi thanh cong, man hinh se hien thi:
echo   tcp://tpa.pinggy.io:XXXXX
echo   Ban chi can gui dia chi va cong XXXXX do cho ban be vao choi!
echo ==================================================
echo.
ssh -p 443 -R0:localhost:7777 -o StrictHostKeyChecking=no tcp@a.pinggy.io
pause
