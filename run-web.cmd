@echo off
cd /d "%~dp0"
java -cp "out;lib\mysql-connector-j-8.4.0.jar" cn.turing.web.PetStoreWebServer
