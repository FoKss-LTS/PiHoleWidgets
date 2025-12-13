#!/usr/bin/env pwsh
# Universal Build Script for PiHole Widgets
# Detects the OS and runs the appropriate build script

param(
    [switch]$Clean = $false,
    [switch]$SkipTests = $false,
    [string]$LinuxType = "deb"  # For Linux: deb, rpm, or both
)

Write-Host "=== PiHole Widgets - Universal Build Script ===" -ForegroundColor Cyan
Write-Host ""

# Detect operating system
$isWindows = $IsWindows -or ($PSVersionTable.PSVersion.Major -lt 6)
$isMacOS = $IsMacOS
$isLinux = $IsLinux

Write-Host "Detecting operating system..." -ForegroundColor Yellow
if ($isWindows) {
    Write-Host "Detected: Windows" -ForegroundColor Green
    Write-Host ""
    
    $params = @()
    if ($Clean) { $params += "-Clean" }
    if ($SkipTests) { $params += "-SkipTests" }
    
    & .\build-windows.ps1 @params
    
} elseif ($isMacOS) {
    Write-Host "Detected: macOS" -ForegroundColor Green
    Write-Host ""
    
    $params = @()
    if ($Clean) { $params += "--clean" }
    if ($SkipTests) { $params += "--skip-tests" }
    
    & chmod +x ./build-macos.sh
    & ./build-macos.sh @params
    
} elseif ($isLinux) {
    Write-Host "Detected: Linux" -ForegroundColor Green
    Write-Host ""
    
    $params = @()
    if ($Clean) { $params += "--clean" }
    if ($SkipTests) { $params += "--skip-tests" }
    $params += "--type"
    $params += $LinuxType
    
    & chmod +x ./build-linux.sh
    & ./build-linux.sh @params
    
} else {
    Write-Host "ERROR: Could not detect operating system." -ForegroundColor Red
    Write-Host "Please run the appropriate build script directly:" -ForegroundColor Yellow
    Write-Host "  - Windows: .\build-windows.ps1" -ForegroundColor White
    Write-Host "  - macOS:   ./build-macos.sh" -ForegroundColor White
    Write-Host "  - Linux:   ./build-linux.sh" -ForegroundColor White
    exit 1
}

