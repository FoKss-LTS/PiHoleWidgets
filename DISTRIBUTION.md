# Distribution Guide for DNSBlocker Widgets

This guide provides comprehensive instructions for building and distributing DNSBlocker Widgets across all major platforms: Windows, macOS, and Linux.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Platform-Specific Builds](#platform-specific-builds)
  - [Windows](#building-for-windows)
  - [macOS](#building-for-macos)
  - [Linux](#building-for-linux)
- [Gradle Commands Reference](#gradle-commands-reference)
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
- *(Portable builds do not require WiX / installer toolchains)*

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

All builds are done through Gradle. No additional wrapper scripts are needed.

### Clone and Build

```bash
# Clone the repository
git clone https://github.com/FoKss-LTS/PiHoleWidgets.git
cd PiHoleWidgets

# Build and test
./gradlew build test
```

### Run the Application

```bash
# Run directly (without creating an installer)
./gradlew run

# Run with verbose logging
./gradlew run -Pdnsbloquer.verbose=true
```

### Create Installers

```bash
# Windows portable (folder with .exe) - Run on Windows
./gradlew jpackageImage -PinstallerType=app-image

# Windows portable ZIP - Run on Windows
./gradlew portableZip -PinstallerType=app-image

# macOS PKG - Run on macOS
./gradlew jpackage -PinstallerType=pkg

# Linux DEB - Run on Linux
./gradlew jpackage -PinstallerType=deb

# Linux RPM - Run on Linux
./gradlew jpackage -PinstallerType=rpm
```

---

## Platform-Specific Builds

### Building for Windows

```bash
# Build the application
.\gradlew.bat build

# Create portable app-image (folder with launcher .exe)
.\gradlew.bat jpackageImage -PinstallerType=app-image

# Create portable ZIP for distribution
.\gradlew.bat portableZip -PinstallerType=app-image
```

#### Output

- **Portable app-image folder:** `build/jpackage/DNSBlocker Widgets/` (contains `DNSBlocker Widgets.exe`)
- **Portable ZIP:** `build/portable/DNSBlocker Widgets-{version}-portable.zip` (contains a versioned folder: `DNSBlocker Widgets-{version}/`)

#### Distribution Notes

- Portable ZIP can be distributed directly (unzip and run)
- No code signing required for personal use (Windows may show SmartScreen for unsigned apps)
- For public distribution, consider signing with a code signing certificate

---

### Building for macOS

```bash
# Build the application
./gradlew build

# Create PKG installer
./gradlew jpackage -PinstallerType=pkg

# Or create DMG image (alternative)
./gradlew jpackage -PinstallerType=dmg
```

#### Output

- **PKG Installer:** `build/jpackage/DNSBlocker Widgets-{version}.pkg`
- **Installation Location:** `/Applications/DNSBlocker Widgets.app`

#### Distribution Notes

- PKG files need to be signed for public distribution
- Users may need to allow the app in **System Preferences > Security & Privacy**
- See [Signing and Notarization](#signing-and-notarization) for details

---

### Building for Linux

```bash
# Build DEB package (Debian/Ubuntu)
./gradlew jpackage -PinstallerType=deb

# Build RPM package (Fedora/RHEL)
./gradlew jpackage -PinstallerType=rpm
```

#### Output

- **DEB Package:** `build/jpackage/dnsblocker-widgets_{version}_amd64.deb`
- **RPM Package:** `build/jpackage/dnsblocker-widgets-{version}.x86_64.rpm`

#### Installation

**Debian/Ubuntu:**

```bash
sudo dpkg -i dnsblocker-widgets_*.deb
sudo apt-get install -f  # Install dependencies if needed
```

**Fedora/RHEL:**

```bash
sudo rpm -i dnsblocker-widgets-*.rpm
```

**Uninstallation:**

```bash
# Debian/Ubuntu
sudo apt-get remove dnsblocker-widgets

# Fedora/RHEL
sudo rpm -e dnsblocker-widgets
```

---

## Gradle Commands Reference

### Basic Commands

| Command | Description |
|---------|-------------|
| `./gradlew clean` | Clean build directory |
| `./gradlew compileJava` | Compile the application |
| `./gradlew test` | Run tests |
| `./gradlew build` | Build JAR file |
| `./gradlew run` | Run the application |
| `./gradlew run -Pdnsbloquer.verbose=true` | Run with verbose logging |
| `./gradlew jlink` | Create runtime image (without installer) |

### Installer Commands

| Platform | Installer Type | Command |
|----------|----------------|---------|
| Windows | app-image (portable) | `./gradlew jpackageImage -PinstallerType=app-image` |
| Windows | portable ZIP | `./gradlew portableZip -PinstallerType=app-image` |
| Windows | MSI (optional) | `./gradlew jpackage -PinstallerType=msi` |
| Windows | EXE (optional) | `./gradlew jpackage -PinstallerType=exe` |
| macOS | PKG | `./gradlew jpackage -PinstallerType=pkg` |
| macOS | DMG | `./gradlew jpackage -PinstallerType=dmg` |
| Linux | DEB | `./gradlew jpackage -PinstallerType=deb` |
| Linux | RPM | `./gradlew jpackage -PinstallerType=rpm` |

---

## GitHub Actions CI/CD

### Automated Builds

The project includes GitHub Actions workflows that automatically build packages for all platforms:

1. **Pull requests and Pushes (CI):** Builds and tests all platforms automatically on every pull request and push to main/develop.
2. **Tagged releases (Release):** Dedicated workflow that builds all platforms, generates checksums, extracts release notes from the changelog, and creates a GitHub release.

### Triggering a Release

```bash
# Tag a release
git tag -a v2.5.1 -m "Release version 2.5.1"
git push origin v2.5.1

# GitHub Actions will automatically:
# 1. Build Windows portable ZIP
# 2. Build macOS PKG
# 3. Build Linux DEB and RPM
# 4. Generate SHA-256 Checksums
# 5. Extract release notes from CHANGELOG.md
# 6. Create a GitHub release with all artifacts and notes
```

### Manually Triggering Workflows

1. Go to the **Actions** tab in your GitHub repository
2. Select **CI** or **Release** workflow
3. Click **Run workflow**
4. Select branch and click **Run workflow**

---

## Distribution Packages

### Package Contents

All installers include:

- DNSBlocker Widgets application
- Java runtime (bundled)
- Required JavaFX libraries
- Application icon

### Package Sizes

Approximate installer sizes:

- **Windows portable (app-image):** ~80-100 MB
- **macOS PKG:** ~80-100 MB
- **Linux DEB/RPM:** ~80-100 MB

*Note: Large size due to bundled Java runtime (no separate Java installation required)*

---

## Signing and Notarization

### Windows Code Signing

To sign the Windows portable launcher executable:

```powershell
# Using signtool from Windows SDK
signtool sign /f certificate.pfx /p password /tr http://timestamp.digicert.com build/jpackage/DNSBlocker Widgets/*.exe

# With hardware token
signtool sign /n "Certificate Name" /tr http://timestamp.digicert.com build/jpackage/DNSBlocker Widgets/*.exe
```

### macOS Code Signing and Notarization

#### Step 1: Sign the PKG

```bash
# Sign with Developer ID Installer certificate
productsign --sign "Developer ID Installer: Your Name (TEAM_ID)" \
  build/jpackage/DNSBlocker Widgets-{version}.pkg \
  build/jpackage/DNSBlocker Widgets-{version}-signed.pkg
```

#### Step 2: Notarize with Apple

```bash
# Submit for notarization
xcrun notarytool submit build/jpackage/DNSBlocker Widgets-{version}-signed.pkg \
  --apple-id "your-email@example.com" \
  --team-id "TEAM_ID" \
  --password "app-specific-password" \
  --wait

# Staple the notarization ticket
xcrun stapler staple build/jpackage/DNSBlocker Widgets-{version}-signed.pkg
```

#### Step 3: Verify

```bash
spctl -a -vvv -t install build/jpackage/DNSBlocker Widgets-{version}-signed.pkg
```

### Linux Package Signing

#### DEB Package Signing

```bash
# Sign with GPG key
dpkg-sig --sign builder build/jpackage/dnsblocker-widgets_*.deb
```

#### RPM Package Signing

```bash
# Sign with GPG key
rpm --addsign build/jpackage/dnsblocker-widgets-*.rpm
```

---

## Troubleshooting

### Common Issues

#### "Java not found" Error

**Solution:**

```bash
# Verify Java installation
java -version

# Set JAVA_HOME (Windows PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"

# Set JAVA_HOME (Windows CMD)
set JAVA_HOME=C:\Program Files\Java\jdk-25

# Set JAVA_HOME (macOS/Linux)
export JAVA_HOME=/path/to/jdk-25
```

#### Windows SmartScreen warning (portable builds)

If Windows shows a SmartScreen warning, this is expected for unsigned apps.
For public distribution, consider code-signing the launcher `.exe` (see [Signing and Notarization](#signing-and-notarization)).

#### Build Directory Lock (Windows)

**Solution:**

```powershell
# Stop Gradle daemon
./gradlew --stop
```

#### "Permission denied" (macOS/Linux)

**Solution:**

```bash
# Make gradlew executable
chmod +x gradlew
```

#### Gatekeeper Warning (macOS)

**Issue:** "App is damaged and can't be opened"

**Solution:**

```bash
# Remove quarantine attribute
xattr -cr "/Applications/DNSBlocker Widgets.app"

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

1. Check the [GitHub Issues](https://github.com/FoKss-LTS/PiHoleWidgets/issues)
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
- [Project Repository](https://github.com/FoKss-LTS/PiHoleWidgets)

---

**Last Updated:** December 2025  
**Version:** 2.5.1
