# Cross-Platform Distribution Setup - Summary

## Overview

PiHole Widgets has been configured for **cross-platform distribution** to Windows, macOS, and Linux. The project uses Gradle commands directly for all builds, with GitHub Actions CI/CD for automated releases.

---

## Build System

All builds are done through **Gradle commands directly** - no wrapper scripts needed.

### Quick Reference

```bash
# Development
./gradlew build test          # Build and test
./gradlew run                 # Run the application

# Distribution
./gradlew portableZip -PinstallerType=app-image   # Windows portable ZIP
./gradlew jpackage -PinstallerType=pkg            # macOS PKG
./gradlew jpackage -PinstallerType=deb            # Linux DEB
./gradlew jpackage -PinstallerType=rpm            # Linux RPM
```

---

## Supported Platforms

| Platform | Installer Type | Min. Version | Status |
|----------|---------------|--------------|--------|
| **Windows** | portable ZIP (app-image) | Win 10 (64-bit) | ✅ Fully Supported |
| **macOS** | PKG | macOS 10.15+ | ✅ Fully Supported |
| **macOS** | DMG | macOS 10.15+ | ✅ Supported |
| **Linux (Debian)** | DEB | Ubuntu 20.04+ | ✅ Fully Supported |
| **Linux (RedHat)** | RPM | Fedora 35+ | ✅ Fully Supported |

---

## GitHub Actions Workflow

**File:** `.github/workflows/build.yml`

**Capabilities:**
- Automated builds on every push to main/master/develop
- Pull request validation
- Multi-platform builds (Windows, macOS, Linux)
- Automatic release creation on version tags
- Artifact storage (30 days)
- Gradle caching for faster builds

**Supported Triggers:**
- Push to branches
- Pull requests
- Tagged releases (`v*`)
- Manual workflow dispatch

---

## Gradle Tasks

### Built-in Tasks

| Task | Description |
|------|-------------|
| `build` | Compile and package the application |
| `test` | Run unit tests |
| `run` | Run the application directly |
| `clean` | Clean build outputs |
| `jlink` | Create runtime image |
| `jpackage` | Create platform installer (requires `-PinstallerType`) |
| `jpackageImage` | Create app-image (no installer) |

### Custom Tasks

| Task | Description |
|------|-------------|
| `portableZip` | Create Windows portable ZIP from app-image |

---

## Distribution Package Details

### Package Contents
All packages include:
- PiHole Widgets application
- Java 25 runtime (bundled)
- JavaFX libraries
- Application icon

### Package Sizes
Approximate sizes: **80-100 MB** per package

*Large size due to bundled Java runtime - ensures zero dependencies for end users*

### Package Naming
- **Windows (portable):** `PiHole-Widgets-{version}-portable.zip`
- **macOS:** `PiHole-Widgets-{version}.pkg`
- **Linux (DEB):** `pihole-widgets_{version}_amd64.deb`
- **Linux (RPM):** `pihole-widgets-{version}.x86_64.rpm`

---

## How to Create a Release

```bash
# 1. Ensure all changes are committed
git add .
git commit -m "Release version 1.5.2"

# 2. Create a version tag
git tag -a v1.5.2 -m "Release version 1.5.2"

# 3. Push changes and tag
git push origin main
git push origin v1.5.2
```

GitHub Actions will automatically:
1. Build Windows portable ZIP
2. Build macOS PKG
3. Build Linux DEB and RPM
4. Create a GitHub release with all artifacts

---

## Key Features

✅ **Cross-platform support** - Windows, macOS, Linux  
✅ **Native packages** - Windows portable, macOS PKG, Linux DEB/RPM  
✅ **Bundled Java runtime** - No separate Java installation needed  
✅ **Automated CI/CD** - GitHub Actions builds all platforms  
✅ **Simple builds** - Direct Gradle commands, no wrapper scripts  
✅ **Comprehensive docs** - Guides for users and developers  

---

## Documentation Quick Links

- **[README.md](README.md)** - Main project documentation
- **[QUICKSTART.md](QUICKSTART.md)** - Quick start guide
- **[DISTRIBUTION.md](DISTRIBUTION.md)** - Detailed build & distribution guide
- **[implementation_plan.md](implementation_plan.md)** - Development roadmap
- **[CHANGELOG.md](CHANGELOG.md)** - Version history

---

## Support

For issues or questions:
- **GitHub Issues:** https://github.com/foxy999/PiHoleWidgets/issues
- **Build problems:** See [DISTRIBUTION.md](DISTRIBUTION.md#troubleshooting)
- **Quick help:** See [QUICKSTART.md](QUICKSTART.md#troubleshooting)

---

## License

GNU General Public License v3.0  
Copyright (C) 2022-2025 Reda ELFARISSI aka foxy999

---

**Status:** ✅ Complete and ready for distribution!  
**Version:** 1.5.2  
**Last Updated:** December 14, 2025
