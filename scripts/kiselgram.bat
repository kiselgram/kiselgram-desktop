@echo off
set DIR=%~dp0
set JAR=%DIR%..\Java\kiselgram-desktop-0.2.0.jar

where /q java
if %ERRORLEVEL% neq 0 (
    echo Kiselgram requires Java 21+.
    echo Download from: https://adoptium.net
    pause
    exit /b 1
)

java --module-path "%DIR%..\Java\javafx-lib" --add-modules javafx.controls,javafx.web -jar "%JAR%"
pause
