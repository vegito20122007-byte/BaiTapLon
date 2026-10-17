@echo off
chcp 65001 > nul
title Auction System - SERVER
echo ========================================================
echo   DANG KHOI DONG SERVER HE THONG DAU GIA TRUC TUYEN...
echo ========================================================

set "M2=%USERPROFILE%\.m2\repository"
set "CP=auction-server\target\classes;auction-common\target\classes;%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar;%M2%\org\slf4j\slf4j-api\2.0.12\slf4j-api-2.0.12.jar;%M2%\ch\qos\logback\logback-classic\1.4.14\logback-classic-1.4.14.jar;%M2%\ch\qos\logback\logback-core\1.4.14\logback-core-1.4.14.jar"

java -cp "%CP%" com.auction.server.ServerApp

pause
