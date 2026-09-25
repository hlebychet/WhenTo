@echo off
setlocal enabledelayedexpansion
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
cd /d "%~dp0"

set "MODE=%~1"
if "%MODE%"=="" set "MODE=device"

if /I "%MODE%"=="test" goto :test
if /I "%MODE%"=="device" goto :device

echo Usage: run.bat [device^|test]
exit /b 1

:test
call gradlew.bat :app:testDebugUnitTest :app:assembleDebug
exit /b %errorlevel%

:device
call gradlew.bat :app:installDebug
if errorlevel 1 exit /b 1
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
"%ADB%" shell am force-stop com.autocalendar
"%ADB%" shell am start -n com.autocalendar/.MainActivity
exit /b 0