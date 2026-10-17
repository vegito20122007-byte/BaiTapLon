@echo off
chcp 65001 > nul
title Auction System - BUILD
echo ========================================================
echo   DANG BIEN DICH HE THONG DAU GIA TRUC TUYEN (BUILD)...
echo ========================================================

set "M2=%USERPROFILE%\.m2\repository"
set "GSON=%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar"
set "SLF4J=%M2%\org\slf4j\slf4j-api\2.0.12\slf4j-api-2.0.12.jar"
set "LOGBACK_C=%M2%\ch\qos\logback\logback-classic\1.4.14\logback-classic-1.4.14.jar"
set "LOGBACK_K=%M2%\ch\qos\logback\logback-core\1.4.14\logback-core-1.4.14.jar"

set "JFX_CTRL=%M2%\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2.jar;%M2%\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar"
set "JFX_GRA=%M2%\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2.jar;%M2%\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar"
set "JFX_BASE=%M2%\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2.jar;%M2%\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar"
set "JFX_FXML=%M2%\org\openjfx\javafx-fxml\21.0.2\javafx-fxml-21.0.2.jar;%M2%\org\openjfx\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar"
set "JFX=%JFX_CTRL%;%JFX_GRA%;%JFX_BASE%;%JFX_FXML%"

echo 1. Bien dich auction-common...
if not exist "auction-common\target\classes" mkdir "auction-common\target\classes"
powershell -Command "$sources = (Get-ChildItem -Recurse -Filter *.java 'auction-common\src\main\java').FullName; javac -encoding UTF-8 -cp '%GSON%' -d 'auction-common\target\classes' $sources"
if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the bien dich auction-common!
    pause
    exit /b %ERRORLEVEL%
)

echo 2. Bien dich auction-server...
if not exist "auction-server\target\classes" mkdir "auction-server\target\classes"
powershell -Command "$sources = (Get-ChildItem -Recurse -Filter *.java 'auction-server\src\main\java').FullName; javac -encoding UTF-8 -cp 'auction-common\target\classes;%GSON%;%SLF4J%;%LOGBACK_C%;%LOGBACK_K%' -d 'auction-server\target\classes' $sources"
if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the bien dich auction-server!
    pause
    exit /b %ERRORLEVEL%
)

echo 3. Bien dich auction-client...
if not exist "auction-client\target\classes" mkdir "auction-client\target\classes"
powershell -Command "$sources = (Get-ChildItem -Recurse -Filter *.java 'auction-client\src\main\java').FullName; javac -encoding UTF-8 -cp 'auction-common\target\classes;%GSON%;%JFX%' -d 'auction-client\target\classes' $sources"
if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the bien dich auction-client!
    pause
    exit /b %ERRORLEVEL%
)

echo 4. Copy resources giao dien...
xcopy /E /I /Y "auction-client\src\main\resources" "auction-client\target\classes" > nul

echo ========================================================
echo   BIEN DICH THANH CONG TOAN BO DU AN!
echo ========================================================
pause
