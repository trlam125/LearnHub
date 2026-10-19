@echo off
where mvn >nul 2>nul
if %errorlevel%==0 (
 call mvn %*
 exit /b
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\maven.ps1" %*
exit /b %errorlevel%
