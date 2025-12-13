#!/usr/bin/env pwsh
# Windows Build Script for PiHole Widgets
# Builds a Windows MSI installer

param(
    [switch]$Clean = $false,
    [switch]$SkipTests = $false
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

# Create jpackage MSI installer
Write-Host "Creating Windows MSI installer..." -ForegroundColor Yellow
& .\gradlew.bat jpackage -PinstallerType=msi
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: jpackage failed." -ForegroundColor Red
    exit 1
}
Write-Host ""

# Find and display the installer location
$installerPath = Get-ChildItem -Path "build\jpackage" -Filter "*.msi" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1

if ($installerPath) {
    Write-Host "=== Build Complete! ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "Windows MSI installer created at:" -ForegroundColor Cyan
    Write-Host "  $($installerPath.FullName)" -ForegroundColor White
    Write-Host ""
    Write-Host "You can now distribute this installer to Windows users." -ForegroundColor Yellow
} else {
    Write-Host "WARNING: Could not find MSI installer in build output." -ForegroundColor Yellow
    Write-Host "Check the build/jpackage directory manually." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Build completed successfully!" -ForegroundColor Green

