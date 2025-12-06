# PowerShell script to fix Windows file locking issues with Gradle build
# This script helps resolve the "Unable to delete directory" error

Write-Host "=== Gradle Build Lock Fix Script ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Stop all Gradle daemons
Write-Host "Step 1: Stopping Gradle daemons..." -ForegroundColor Yellow
try {
    & .\gradlew.bat --stop 2>&1 | Out-Null
    Write-Host "  ✓ Gradle daemons stopped" -ForegroundColor Green
} catch {
    Write-Host "  ⚠ Could not stop Gradle daemons (may not be running)" -ForegroundColor Yellow
}

# Step 2: Check for processes locking the build directory
Write-Host ""
Write-Host "Step 2: Checking for processes that might lock build files..." -ForegroundColor Yellow

$buildPath = "E:\OneDrive\Documents\GitHub\PiHoleWidgets\build"
$lockedFiles = @()

# Check for Java processes
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Write-Host "  ⚠ Found Java processes running:" -ForegroundColor Yellow
    $javaProcesses | ForEach-Object { Write-Host "    - $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Gray }
} else {
    Write-Host "  ✓ No Java processes found" -ForegroundColor Green
}

# Check for Gradle processes
$gradleProcesses = Get-Process -Name "gradle*" -ErrorAction SilentlyContinue
if ($gradleProcesses) {
    Write-Host "  ⚠ Found Gradle processes running:" -ForegroundColor Yellow
    $gradleProcesses | ForEach-Object { Write-Host "    - $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Gray }
} else {
    Write-Host "  ✓ No Gradle processes found" -ForegroundColor Green
}

# Step 3: Attempt to remove build directory with retry logic
Write-Host ""
Write-Host "Step 3: Attempting to remove build directory..." -ForegroundColor Yellow

if (Test-Path $buildPath) {
    $maxRetries = 3
    $retryCount = 0
    $success = $false
    
    while ($retryCount -lt $maxRetries -and -not $success) {
        $retryCount++
        Write-Host "  Attempt $retryCount of $maxRetries..." -ForegroundColor Gray
        
        try {
            # Use Remove-Item with -Force and -Recurse, and -ErrorAction Stop to catch errors
            Remove-Item -Path $buildPath -Recurse -Force -ErrorAction Stop
            Write-Host "  ✓ Build directory removed successfully" -ForegroundColor Green
            $success = $true
        } catch {
            if ($retryCount -lt $maxRetries) {
                Write-Host "  ⚠ Failed to remove directory, waiting 2 seconds before retry..." -ForegroundColor Yellow
                Start-Sleep -Seconds 2
            } else {
                Write-Host "  ✗ Failed to remove build directory after $maxRetries attempts" -ForegroundColor Red
                Write-Host ""
                Write-Host "Manual steps to resolve:" -ForegroundColor Cyan
                Write-Host "  1. Close any IDEs (IntelliJ, VS Code, etc.) that might have the project open" -ForegroundColor White
                Write-Host "  2. Check if OneDrive is syncing files in the build directory" -ForegroundColor White
                Write-Host "  3. Temporarily pause OneDrive sync for this folder" -ForegroundColor White
                Write-Host "  4. Check Windows Defender or antivirus software" -ForegroundColor White
                Write-Host "  5. Restart your computer if the issue persists" -ForegroundColor White
                Write-Host ""
                Write-Host "  Or manually delete: $buildPath" -ForegroundColor Yellow
            }
        }
    }
} else {
    Write-Host "  ✓ Build directory does not exist (already clean)" -ForegroundColor Green
}

# Step 4: Clean Gradle cache (optional)
Write-Host ""
Write-Host "Step 4: Cleaning Gradle cache..." -ForegroundColor Yellow
$gradleUserHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { "$env:USERPROFILE\.gradle" }
$daemonCache = Join-Path $gradleUserHome "daemon"

if (Test-Path $daemonCache) {
    try {
        Get-ChildItem -Path $daemonCache -Filter "*.lock" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
        Write-Host "  ✓ Gradle daemon lock files cleaned" -ForegroundColor Green
    } catch {
        Write-Host "  ⚠ Could not clean all daemon locks" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== Script Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "You can now try running: .\gradlew.bat clean build" -ForegroundColor Green

