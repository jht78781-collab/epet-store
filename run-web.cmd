@echo off
cd /d "%~dp0"
if exist "target\epet-store.jar" (
    java -jar "target\epet-store.jar"
) else (
    java -cp "out;target\classes;lib\*" cn.turing.web.PetStoreWebServer
)
