@echo off
chcp 65001 > nul
title Auction System - RUN TESTS
echo ========================================================
echo   CHAY TOAN BO 18 JUNIT 5 TESTS QUA MAVEN...
echo ========================================================

where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    call mvn test
) else (
    call mvnw.cmd test
)

echo ========================================================
echo   HOAN THANH KIEM THU TOAN BO DU AN!
echo ========================================================
pause
