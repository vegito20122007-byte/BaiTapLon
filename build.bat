@echo off
chcp 65001 > nul
title Auction System - BUILD
echo ========================================================
echo   DANG BIEN DICH VA DONG GOI HE THONG DAU GIA TRUC TUYEN...
echo ========================================================

REM 1. Kiem tra Java
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong tim thay Java trong he thong!
    echo Vui long cai dat JDK (Java 17 tro len).
    pause
    exit /b 1
)

REM 2. Su dung mvn neu co hoac tu dong dung mvnw (Maven Wrapper)
where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo Dang su dung Maven toan cuc tren may...
    call mvn clean package -DskipTests
) else (
    echo Dang su dung Maven Wrapper (mvnw) di kem du an...
    call mvnw.cmd clean package -DskipTests
)

if %ERRORLEVEL% neq 0 (
    echo ========================================================
    echo   [LOI] BIEN DICH THAT BAI!
    echo ========================================================
    pause
    exit /b %ERRORLEVEL%
)

echo ========================================================
echo   BIEN DICH VA DONG GOI THANH CONG TOAN BO DU AN!
echo   Tat ca thu vien da duoc copy san vao target/lib!
echo ========================================================
pause
