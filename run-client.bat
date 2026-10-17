@echo off
chcp 65001 > nul
title Auction System - CLIENT GUI
echo ========================================================
echo   DANG KHOI DONG GIAO DIEN CLIENT JAVAFX...
echo ========================================================

set "M2=%USERPROFILE%\.m2\repository"
set "CP=auction-client\target\classes;auction-common\target\classes;%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar;%M2%\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2.jar;%M2%\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar;%M2%\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2.jar;%M2%\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar;%M2%\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2.jar;%M2%\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;%M2%\org\openjfx\javafx-fxml\21.0.2\javafx-fxml-21.0.2.jar;%M2%\org\openjfx\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar"

java -cp "%CP%" com.auction.client.Launcher

pause
