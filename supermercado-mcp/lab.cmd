@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0lab.ps1" %*
exit /b %ERRORLEVEL%
