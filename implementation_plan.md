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

### Phase 3 — Distribution
- ✅ Cross-platform build scripts (`build.ps1`, `build.sh`, and platform scripts)
- ✅ Documentation for building/distribution (`DISTRIBUTION.md`, `QUICKSTART.md`, README updates)

## In Progress / Next Changes

### Windows distribution: switch from installer to portable
- 🔄 Replace Windows MSI installer output with **portable app-image** output
  - Goal: user downloads/unzips and runs the included `.exe` (no installation)
  - Implementation: use Gradle `jpackageImage` with `installerType=app-image`
  - Output: a portable folder containing the launcher `.exe` (optionally zipped for release)

## Backlog (Future Ideas)
- Multiple Pi-hole support
- Themes
- Tray support
- Enable/disable Pi-hole from widget


