#!/bin/bash
# macOS Build Script for PiHole Widgets
# Builds a macOS PKG installer

set -e

CLEAN=false
SKIP_TESTS=false

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
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--clean] [--skip-tests]"
            exit 1
            ;;
    esac
done

echo "=== PiHole Widgets - macOS Build ==="
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

# Create jpackage PKG installer
echo "Creating macOS PKG installer..."
./gradlew jpackage -PinstallerType=pkg
echo ""

# Find and display the installer location
INSTALLER=$(find build/jpackage -name "*.pkg" -type f | head -n 1)

if [ -n "$INSTALLER" ]; then
    echo "=== Build Complete! ==="
    echo ""
    echo "macOS PKG installer created at:"
    echo "  $INSTALLER"
    echo ""
    echo "You can now distribute this installer to macOS users."
    echo ""
    echo "Note: The PKG may need to be signed for distribution outside the App Store."
    echo "Use 'productsign' to sign the package for Gatekeeper compatibility."
else
    echo "WARNING: Could not find PKG installer in build output."
    echo "Check the build/jpackage directory manually."
fi

echo ""
echo "Build completed successfully!"

