# Distribution Guide for PiHole Widgets

This guide provides comprehensive instructions for building and distributing PiHole Widgets across all major platforms: Windows, macOS, and Linux.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Platform-Specific Builds](#platform-specific-builds)
  - [Windows](#building-for-windows)
  - [macOS](#building-for-macos)
  - [Linux](#building-for-linux)
- [Manual Build Commands](#manual-build-commands)
- [GitHub Actions CI/CD](#github-actions-cicd)
- [Distribution Packages](#distribution-packages)
- [Signing and Notarization](#signing-and-notarization)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### All Platforms

1. **Java Development Kit (JDK) 25**
   - Download from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
   - Set `JAVA_HOME` environment variable to your JDK installation path
   - Verify installation: `java -version`

2. **Git** (for cloning the repository)
   - Download from [git-scm.com](https://git-scm.com/)

### Platform-Specific Requirements

#### Windows
- **Windows 10 or later** (64-bit)
- **WiX Toolset 3.11+** (for MSI installers)
  - Download from [WiX Toolset](https://wixtoolset.org/releases/)
  - Add WiX bin directory to PATH
- **PowerShell 5.1+** or **PowerShell Core 7+**

#### macOS
- **macOS 10.15 (Catalina) or later**
- **Xcode Command Line Tools**
  ```bash
  xcode-select --install
  ```
- **Developer ID certificate** (optional, for signed packages)

#### Linux
- **Debian/Ubuntu:**
  ```bash
  sudo apt-get install fakeroot
  ```
  
- **Fedora/RHEL/CentOS:**
  ```bash
  sudo dnf install rpm-build
  ```

---

## Quick Start

### Using the Universal Build Scripts

The easiest way to build for your platform is to use the universal build scripts:

**Windows (PowerShell):**
```powershell
.\build.ps1
```

**macOS/Linux (Bash):**
```bash
chmod +x build.sh
./build.sh
```

### Build Options

- `--clean` or `-Clean`: Clean previous builds before building
- `--skip-tests` or `-SkipTests`: Skip running tests
- `--linux-type` or `-LinuxType`: Specify Linux package type (`deb`, `rpm`, or `both`)

**Examples:**

```powershell
# Windows: Clean build with tests
.\build.ps1 -Clean

# Windows: Skip tests
.\build.ps1 -SkipTests

# Windows: Clean build without tests
.\build.ps1 -Clean -SkipTests
```

```bash
# Linux: Build both DEB and RPM
./build.sh --clean --linux-type both

# macOS: Quick build without cleaning
./build.sh --skip-tests
```

---

## Platform-Specific Builds

### Building for Windows

#### Method 1: Using the build script (Recommended)

```powershell
.\build-windows.ps1
```

#### Method 2: Manual Gradle commands

```powershell
# Build the application
.\gradlew.bat build

# Create MSI installer
.\gradlew.bat jpackage -PinstallerType=msi
```

#### Output
- **MSI Installer:** `build/jpackage/PiHole-Widgets-{version}.msi`
- **Installation Location:** `C:\Users\{username}\AppData\Local\PiHole-Widgets\`

#### Distribution Notes
- MSI installers can be distributed directly
- No code signing required for personal use
- For public distribution, consider signing with a code signing certificate

---

### Building for macOS

#### Method 1: Using the build script (Recommended)

```bash
chmod +x build-macos.sh
./build-macos.sh
```

#### Method 2: Manual Gradle commands

```bash
# Build the application
./gradlew build

# Create PKG installer
./gradlew jpackage -PinstallerType=pkg

# Or create DMG image (alternative)
./gradlew jpackage -PinstallerType=dmg
```

#### Output
- **PKG Installer:** `build/jpackage/PiHole-Widgets-{version}.pkg`
- **Installation Location:** `/Applications/PiHole Widgets.app`

#### Distribution Notes
- PKG files need to be signed for public distribution
- Users may need to allow the app in **System Preferences > Security & Privacy**
- See [Signing and Notarization](#signing-and-notarization) for details

---

### Building for Linux

#### Method 1: Using the build script (Recommended)

```bash
chmod +x build-linux.sh

# Build DEB package (Debian/Ubuntu)
./build-linux.sh --type deb

# Build RPM package (Fedora/RHEL)
./build-linux.sh --type rpm

# Build both
./build-linux.sh --type both
```

#### Method 2: Manual Gradle commands

```bash
# Build DEB package
./gradlew build
./gradlew jpackage -PinstallerType=deb

# Build RPM package
./gradlew clean build
./gradlew jpackage -PinstallerType=rpm
```

#### Output
- **DEB Package:** `build/jpackage/pihole-widgets_{version}_amd64.deb`
- **RPM Package:** `build/jpackage/pihole-widgets-{version}.x86_64.rpm`

#### Installation

**Debian/Ubuntu:**
```bash
sudo dpkg -i pihole-widgets_*.deb
sudo apt-get install -f  # Install dependencies if needed
```

**Fedora/RHEL:**
```bash
sudo rpm -i pihole-widgets-*.rpm
```

**Uninstallation:**
```bash
# Debian/Ubuntu
sudo apt-get remove pihole-widgets

# Fedora/RHEL
sudo rpm -e pihole-widgets
```

---

## Manual Build Commands

### Basic Gradle Commands

```bash
# Clean build directory
./gradlew clean

# Compile the application
./gradlew compileJava

# Run tests
./gradlew test

# Build JAR file
./gradlew build

# Run the application
./gradlew run

# Run with verbose logging
./gradlew run -Ppihole.verbose=true

# Create runtime image (without installer)
./gradlew jlink

# Create installer (requires -PinstallerType)
./gradlew jpackage -PinstallerType=<type>
```

### Supported Installer Types

| Platform | Installer Type | Command |
|----------|---------------|---------|
| Windows  | MSI           | `./gradlew jpackage -PinstallerType=msi` |
| Windows  | EXE           | `./gradlew jpackage -PinstallerType=exe` |
| macOS    | PKG           | `./gradlew jpackage -PinstallerType=pkg` |
| macOS    | DMG           | `./gradlew jpackage -PinstallerType=dmg` |
| Linux    | DEB           | `./gradlew jpackage -PinstallerType=deb` |
| Linux    | RPM           | `./gradlew jpackage -PinstallerType=rpm` |

---

## GitHub Actions CI/CD

### Automated Builds

The project includes GitHub Actions workflows that automatically build packages for all platforms:

1. **Push to main/master/develop branches:** Builds all platforms
2. **Pull requests:** Builds and tests all platforms
3. **Tagged releases (v*):** Builds all platforms and creates a GitHub release

### Triggering a Build

```bash
# Tag a release
git tag -a v1.5.2 -m "Release version 1.5.2"
git push origin v1.5.2

# GitHub Actions will automatically:
# 1. Build Windows MSI
# 2. Build macOS PKG
# 3. Build Linux DEB and RPM
# 4. Create a GitHub release with all artifacts
```

### Manually Triggering Workflows

1. Go to the **Actions** tab in your GitHub repository
2. Select **Build and Release** workflow
3. Click **Run workflow**
4. Select branch and click **Run workflow**

---

## Distribution Packages

### Package Contents

All installers include:
- PiHole Widgets application
- Java runtime (bundled)
- Required JavaFX libraries
- Application icon
- Desktop shortcuts (platform-dependent)
- Start menu entries (Windows)

### Package Sizes

Approximate installer sizes:
- **Windows MSI:** ~80-100 MB
- **macOS PKG:** ~80-100 MB
- **Linux DEB/RPM:** ~80-100 MB

*Note: Large size due to bundled Java runtime (no separate Java installation required)*

---

## Signing and Notarization

### Windows Code Signing

To sign Windows MSI installers:

```powershell
# Using signtool from Windows SDK
signtool sign /f certificate.pfx /p password /tr http://timestamp.digicert.com build/jpackage/*.msi

# With hardware token
signtool sign /n "Certificate Name" /tr http://timestamp.digicert.com build/jpackage/*.msi
```

### macOS Code Signing and Notarization

#### Step 1: Sign the PKG

```bash
# Sign with Developer ID Installer certificate
productsign --sign "Developer ID Installer: Your Name (TEAM_ID)" \
  build/jpackage/PiHole-Widgets-1.5.2.pkg \
  build/jpackage/PiHole-Widgets-1.5.2-signed.pkg
```

#### Step 2: Notarize with Apple

```bash
# Submit for notarization
xcrun notarytool submit build/jpackage/PiHole-Widgets-1.5.2-signed.pkg \
  --apple-id "your-email@example.com" \
  --team-id "TEAM_ID" \
  --password "app-specific-password" \
  --wait

# Staple the notarization ticket
xcrun stapler staple build/jpackage/PiHole-Widgets-1.5.2-signed.pkg
```

#### Step 3: Verify

```bash
spctl -a -vvv -t install build/jpackage/PiHole-Widgets-1.5.2-signed.pkg
```

### Linux Package Signing

#### DEB Package Signing

```bash
# Sign with GPG key
dpkg-sig --sign builder build/jpackage/pihole-widgets_*.deb
```

#### RPM Package Signing

```bash
# Sign with GPG key
rpm --addsign build/jpackage/pihole-widgets-*.rpm
```

---

## Troubleshooting

### Common Issues

#### "Java not found" Error

**Solution:**
```bash
# Verify Java installation
java -version

# Set JAVA_HOME (Windows)
setx JAVA_HOME "C:\Program Files\Java\jdk-25"

# Set JAVA_HOME (macOS/Linux)
export JAVA_HOME=/path/to/jdk-25
```

#### "WiX Toolset not found" (Windows)

**Solution:**
1. Download and install [WiX Toolset](https://wixtoolset.org/releases/)
2. Add WiX bin directory to PATH:
   ```powershell
   $env:PATH += ";C:\Program Files (x86)\WiX Toolset v3.11\bin"
   ```

#### Build Directory Lock (Windows)

**Solution:**
```powershell
# Stop processes locking the build directory
.\fix-build-locks.ps1

# Or manually
./gradlew --stop
# Pause OneDrive sync if applicable
```

#### "Permission denied" (macOS/Linux)

**Solution:**
```bash
# Make scripts executable
chmod +x build.sh
chmod +x build-macos.sh
chmod +x build-linux.sh
chmod +x gradlew
```

#### Gatekeeper Warning (macOS)

**Issue:** "App is damaged and can't be opened"

**Solution:**
```bash
# Remove quarantine attribute
xattr -cr "/Applications/PiHole Widgets.app"

# Or allow the app in System Preferences > Security & Privacy
```

#### Missing Dependencies (Linux)

**Debian/Ubuntu:**
```bash
sudo apt-get update
sudo apt-get install fakeroot binutils
```

**Fedora/RHEL:**
```bash
sudo dnf install rpm-build
```

### Getting Help

If you encounter issues:
1. Check the [GitHub Issues](https://github.com/foxy999/PiHoleWidgets/issues)
2. Review build logs in `build/` directory
3. Enable verbose logging: `./gradlew --info build`
4. Open a new issue with:
   - OS and version
   - Java version
   - Complete error message
   - Steps to reproduce

---

## Additional Resources

- [Gradle Documentation](https://docs.gradle.org/)
- [jpackage Guide](https://docs.oracle.com/en/java/javase/25/jpackage/)
- [JavaFX Documentation](https://openjfx.io/)
- [Project Repository](https://github.com/foxy999/PiHoleWidgets)

---

**Last Updated:** December 2025  
**Version:** 1.5.2

