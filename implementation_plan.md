# PiHole Widgets — Implementation Plan / Roadmap

This file tracks planned work and what has been implemented.

## Current Status (as of 2025-12-14)

### Phase 1 — Core Application (JavaFX)
- ✅ Widget UI and controller logic
- ✅ Configuration UI (`Configuration.fxml`) and persistence
- ✅ Pi-hole API integration (`services/pihole/PiHoleHandler.java`)
- ✅ HTTP helper utilities (`helpers/HttpClientUtil.java`)
- ✅ Unit tests for core helpers and Pi-hole handler

### Phase 2 — Build + Packaging Foundation (Gradle)
- ✅ Modular build (`module-info.java`)
- ✅ JavaFX plugin setup
- ✅ jlink runtime image support
- ✅ jpackage support (cross-platform packaging)
- ✅ `portableZip` Gradle task for Windows portable distribution

### Phase 3 — Distribution
- ✅ Simplified build process using Gradle commands directly
- ✅ Removed wrapper scripts (build.ps1, build.sh, build-windows.ps1, etc.)
- ✅ Documentation for building/distribution (`DISTRIBUTION.md`, `QUICKSTART.md`, README updates)
- ✅ GitHub Actions CI/CD workflow

### Build Commands Reference
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

## Backlog (Future Ideas)
- Multiple Pi-hole support
- Themes
- Tray support
- Enable/disable Pi-hole from widget

