#!/bin/bash
# Linux Build Script for PiHole Widgets
# Builds DEB (Debian/Ubuntu) and RPM (Fedora/RedHat) packages

set -e

CLEAN=false
SKIP_TESTS=false
INSTALLER_TYPE="deb"  # Default to deb

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --clean)
            CLEAN=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --type)
            INSTALLER_TYPE="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--clean] [--skip-tests] [--type deb|rpm|both]"
            exit 1
            ;;
    esac
done

echo "=== PiHole Widgets - Linux Build ==="
echo ""

# Check Java installation
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found. Please install JDK 25."
    exit 1
fi
java -version
echo ""

# Clean build if requested
if [ "$CLEAN" = true ]; then
    echo "Cleaning previous build..."
    ./gradlew clean
    echo ""
fi

# Run tests unless skipped
if [ "$SKIP_TESTS" = false ]; then
    echo "Running tests..."
    ./gradlew test
    echo "Tests passed!"
    echo ""
fi

# Build the application
echo "Building application..."
./gradlew build
echo "Build successful!"
echo ""

# Function to build a specific installer type
build_installer() {
    local type=$1
    echo "Creating Linux $type package..."
    ./gradlew jpackage -PinstallerType=$type
    echo ""
    
    # Find and display the installer location
    local ext=$type
    local installer=$(find build/jpackage -name "*.$ext" -type f | head -n 1)
    
    if [ -n "$installer" ]; then
        echo "=== $type Package Created! ==="
        echo "  $installer"
        echo ""
    else
        echo "WARNING: Could not find $ext package in build output."
    fi
}

# Build requested installer types
if [ "$INSTALLER_TYPE" = "both" ]; then
    build_installer "deb"
    build_installer "rpm"
elif [ "$INSTALLER_TYPE" = "deb" ] || [ "$INSTALLER_TYPE" = "rpm" ]; then
    build_installer "$INSTALLER_TYPE"
else
    echo "ERROR: Invalid installer type: $INSTALLER_TYPE"
    echo "Valid types: deb, rpm, both"
    exit 1
fi

echo "=== Build Complete! ==="
echo ""
echo "Linux packages created in build/jpackage/"
echo ""
echo "Distribution:"
echo "  - DEB package: For Debian, Ubuntu, Linux Mint, etc."
echo "  - RPM package: For Fedora, RedHat, CentOS, openSUSE, etc."
echo ""
echo "Build completed successfully!"

