#!/bin/bash
# Universal Build Script for PiHole Widgets (Bash version)
# Detects the OS and runs the appropriate build script

set -e

CLEAN=false
SKIP_TESTS=false
LINUX_TYPE="deb"

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
        --linux-type)
            LINUX_TYPE="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--clean] [--skip-tests] [--linux-type deb|rpm|both]"
            exit 1
            ;;
    esac
done

echo "=== PiHole Widgets - Universal Build Script ==="
echo ""

# Detect operating system
echo "Detecting operating system..."
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
    echo "Detected: Windows"
    echo ""
    
    PARAMS=""
    if [ "$CLEAN" = true ]; then PARAMS="$PARAMS -Clean"; fi
    if [ "$SKIP_TESTS" = true ]; then PARAMS="$PARAMS -SkipTests"; fi
    
    ./build-windows.ps1 $PARAMS
    
elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Detected: macOS"
    echo ""
    
    chmod +x ./build-macos.sh
    
    PARAMS=""
    if [ "$CLEAN" = true ]; then PARAMS="$PARAMS --clean"; fi
    if [ "$SKIP_TESTS" = true ]; then PARAMS="$PARAMS --skip-tests"; fi
    
    ./build-macos.sh $PARAMS
    
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "Detected: Linux"
    echo ""
    
    chmod +x ./build-linux.sh
    
    PARAMS=""
    if [ "$CLEAN" = true ]; then PARAMS="$PARAMS --clean"; fi
    if [ "$SKIP_TESTS" = true ]; then PARAMS="$PARAMS --skip-tests"; fi
    PARAMS="$PARAMS --type $LINUX_TYPE"
    
    ./build-linux.sh $PARAMS
    
else
    echo "ERROR: Could not detect operating system: $OSTYPE"
    echo "Please run the appropriate build script directly:"
    echo "  - Windows: ./build-windows.ps1"
    echo "  - macOS:   ./build-macos.sh"
    echo "  - Linux:   ./build-linux.sh"
    exit 1
fi

