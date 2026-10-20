@echo off
chcp 65001 > nul
title Auction System - CLIENT GUI

REM 1. Kiem tra xem may da cai dat Java (JDK 17+) chua
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ========================================================
    echo   [LOI] KHONG TIM THAY JAVA TRONG HE THONG!
    echo   Vui long cai dat JDK [Java 17 tro len] de khoi dong.
    echo   Link tai: https://www.oracle.com/java/technologies/downloads/
    echo ========================================================
    pause
    exit /b 1
)

REM 2. Neu he thong chua duoc build thi tu dong build qua Maven Wrapper
if not exist "auction-client\target\auction-client-1.0.0-SNAPSHOT.jar" (
    echo ========================================================
    echo   PHAT HIEN MAY MOI CHAY LAN DAU TIEN!
    echo   DANG TU DONG TAI THU VIEN VA BIEN DICH HE THONG...
    echo   [Qua trinh nay chi chay 1 lan duy nhat khi vua clone ve]
    echo ========================================================
    
    where mvn >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        call mvn package -DskipTests
    ) else (
        call mvnw.cmd package -DskipTests
    )

    if not exist "auction-client\target\auction-client-1.0.0-SNAPSHOT.jar" (
        echo ========================================================
        echo   [LOI] Khong the tu dong bien dich du an!
        echo   Vui long dam bao may co ket noi Internet de tai dependency.
        echo ========================================================
        pause
        exit /b 1
    )
)

echo ========================================================
echo   DANG KHOI DONG GIAO DIEN CLIENT JAVAFX...
echo ========================================================

java -cp "auction-client\target\auction-client-1.0.0-SNAPSHOT.jar;auction-client\target\lib\*" com.auction.client.Launcher

pause
