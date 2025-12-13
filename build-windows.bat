@echo off
REM Quick build script for Windows users
REM Double-click this file to build the Windows MSI installer

echo ============================================
echo    PiHole Widgets - Windows Build
echo ============================================
echo.

REM Check if PowerShell is available
where pwsh >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Using PowerShell Core...
    pwsh -ExecutionPolicy Bypass -File build-windows.ps1
) else (
    where powershell >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        echo Using Windows PowerShell...
        powershell -ExecutionPolicy Bypass -File build-windows.ps1
    ) else (
        echo ERROR: PowerShell not found!
        echo Please install PowerShell to build this project.
        pause
        exit /b 1
    )
)

echo.
echo ============================================
echo Build script completed!
echo ============================================
pause

