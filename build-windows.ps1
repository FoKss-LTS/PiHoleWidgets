#!/usr/bin/env pwsh
# Windows Build Script for PiHole Widgets
# Builds a Windows *portable* app-image (folder with a launcher .exe)

param(
    [switch]$Clean = $false,
    [switch]$SkipTests = $false,
    [bool]$Zip = $true
)

Write-Host "=== PiHole Widgets - Windows Build ===" -ForegroundColor Cyan
Write-Host ""

# Check Java installation
Write-Host "Checking Java installation..." -ForegroundColor Yellow
$javaVersion = & java -version 2>&1 | Select-Object -First 1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Java not found. Please install JDK 25 and set JAVA_HOME." -ForegroundColor Red
    exit 1
}
Write-Host "Java found: $javaVersion" -ForegroundColor Green
Write-Host ""

# Clean build if requested
if ($Clean) {
    Write-Host "Cleaning previous build..." -ForegroundColor Yellow
    & .\gradlew.bat clean
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Clean failed." -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}

# Run tests unless skipped
if (-not $SkipTests) {
    Write-Host "Running tests..." -ForegroundColor Yellow
    & .\gradlew.bat test
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Tests failed." -ForegroundColor Red
        exit 1
    }
    Write-Host "Tests passed!" -ForegroundColor Green
    Write-Host ""
}

# Build the application
Write-Host "Building application..." -ForegroundColor Yellow
& .\gradlew.bat build
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed." -ForegroundColor Red
    exit 1
}
Write-Host "Build successful!" -ForegroundColor Green
Write-Host ""

# Create jpackage portable app-image
Write-Host "Creating Windows portable app-image..." -ForegroundColor Yellow
& .\gradlew.bat jpackageImage -PinstallerType=app-image
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: jpackage failed." -ForegroundColor Red
    exit 1
}
Write-Host ""

# Find the app-image directory (portable bundle)
$appImageDir = Get-ChildItem -Path "build\jpackage" -Directory -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $appImageDir) {
    Write-Host "WARNING: Could not find portable app-image directory in build output." -ForegroundColor Yellow
    Write-Host "Check the build/jpackage directory manually." -ForegroundColor Yellow
    exit 0
}

# The launcher .exe is at the root of the app-image folder
$launcherExe = Get-ChildItem -Path $appImageDir.FullName -File -Filter "*.exe" -ErrorAction SilentlyContinue |
    Sort-Object Length -Descending |
    Select-Object -First 1

Write-Host "=== Build Complete! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Portable app-image folder created at:" -ForegroundColor Cyan
Write-Host "  $($appImageDir.FullName)" -ForegroundColor White

if ($launcherExe) {
    Write-Host ""
    Write-Host "Run the widget using:" -ForegroundColor Cyan
    Write-Host "  $($launcherExe.FullName)" -ForegroundColor White
}

if ($Zip) {
    $portableOutDir = Join-Path -Path "build" -ChildPath "portable"
    New-Item -ItemType Directory -Force -Path $portableOutDir | Out-Null

    $zipPath = Join-Path -Path $portableOutDir -ChildPath "PiHole-Widgets-windows-portable.zip"
    if (Test-Path $zipPath) { Remove-Item -Force $zipPath }

    Write-Host ""
    Write-Host "Creating portable ZIP..." -ForegroundColor Yellow
    Compress-Archive -Path $appImageDir.FullName -DestinationPath $zipPath -Force
    Write-Host "Portable ZIP created at:" -ForegroundColor Cyan
    Write-Host "  $(Resolve-Path $zipPath)" -ForegroundColor White
}

Write-Host ""
Write-Host "Build completed successfully!" -ForegroundColor Green

