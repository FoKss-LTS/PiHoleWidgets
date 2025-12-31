# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.5.1] - 2025-12-31

### Added

- **AdGuard Home support**
  - Added a **Platform** selection (Pi-hole / AdGuard Home) in the configuration UI and configuration model.
  - Implemented an `AdGuardHomeHandler` using **HTTP Basic Authentication** (username/password).
  - Added handler routing via `DnsBlockerHandlerFactory` to select the correct implementation per platform.
  


### Changed

- **Branding & packaging names**: Standardized the application/launcher/installer naming to **DNSBlocker Widgets**. Linux packages now use the space-free name `dnsblocker-widgets`.
- **Configuration terminology**: Standardized the configuration field naming to **Password** (Pi-hole uses an *App Password*; AdGuard Home uses *Username/Password*).


## [2.0.1] - 2025-12-21

### Changed

- **Version Unification**: Unified application version management. Code and Build now use `gradle.properties` as the single source of truth.
- **Build System**: Fixed `jpackage` Gradle task to prevent cross-platform build errors on Windows (e.g., trying to build DEB on Windows).

## [2.0.0] - 2025-12-21

### Added

- **System Tray Integration**
  - System tray icon with popup menu
  - Hide to tray functionality
  - Show/restore from tray (double-click or menu)
  - Tray context menu (Show, Hide, Settings, Exit)

- **Pi-hole Control Features**
  - Enable/Disable DNS blocking directly from widget
  - DNS blocking status indicator (LED tile)
  - "Refresh All Now" context menu option
  - HTTP/HTTPS protocol support

- **Themes & UI Enhancements**
  - Dark theme (`dark-theme.css`)
  - Light theme (`light-theme.css`)
  - ThemeManager for dynamic theme switching
  - Theme selection in configuration UI

- **Distribution & Packaging**
  - jlink runtime image support
  - jpackage support for cross-platform packaging (Windows, macOS, Linux)
  - `portableZip` Gradle task for Windows portable distribution
  - GitHub Actions CI/CD workflow
  - Updated documentation (`DISTRIBUTION.md`, `QUICKSTART.md`, README)

### Changed

- **Java 17 → Java 25 Migration**
  - Raised the toolchain to Java 25 with JavaFX 25 and Gradle 9.2.1
  - Updated Java toolchain configuration to use Java 25
  - Added compiler warnings for deprecation and unchecked code
  - Replaced deprecated `java.util.Calendar` with `java.time.Year` for copyright year display
  - Fixed module-info.java to export `domain.configuration` package for proper module access

- **Dependency Updates**
  - ControlsFX: 11.1.0 → 11.2.2
  - FormsFX: 11.3.2 → 11.6.0
  - ValidatorFX: 0.1.13 → 0.6.1
  - Ikonli JavaFX: 12.2.0 → 12.4.0
  - TilesFX: 11.48 → 21.0.9
  - JetBrains Annotations: 20.1.0 → 26.0.2
  - JUnit: 5.8.1 → 5.10.2
  - Migrated from json-simple 1.1 to Jackson 2.18.2 for better Java 25 compatibility

- **Build System**
  - Updated Gradle wrapper from 7.3.1 to 9.2.1
  - Updated JavaFX plugin from 0.0.13 to 0.1.0
  - Updated jlink plugin from 2.26.0 to 3.1.3
  - Simplified build process using Gradle commands directly
  - Removed legacy wrapper scripts (build.ps1, build.sh, build-windows.ps1, etc.)
  - Documented the new Java requirement and Gradle commands in `README.md`

- **Code Quality**
  - Optimized API Calls
  - Handle Timeout
  - Removed deprecated API usage
  - Improved module system compatibility

## Ver [1.5.2] - 02-13-2022

### Changed

- Added Support for Pihole 5.9
- Added Settings helpers.
- Updated Changelog and Readme

## Ver [1.5.1] - 01-27-2022

### Added

- Optimizations if piholes not available.
- New Layouts.
- Added option to change size.
- Added Top X Tile.
- Added Setting option to context menu.
- Added port in Settings

### Changed

- RMB works on the whole interface.
- Optimised code.
- Updated README.md

### Fixed

- Fixed bug if saving while widget settings doesn't exist.

## Ver [1.0.1] - 01-19-2022

### Fixed

- Configuration Interface appears if configuration not available.
- Display 0% if no configuration found.
- Fixed Apply button => save and apply

## Ver [1.0.0] - 01-19-2022

### Added

- Configuration UI
- Hot Configuration swapping

### Fixed

- Bug if DNS unreachable
- Last gravity update bug fix
- Fixed some bugs

### Changed

- Organized Code
- Optimized API calls
- % calculated from two Pi-Holes
- IP Addresses of two Pi-Holes
- Updated README.md

## Ver [0.9.0] - 01-17-2022

### Added

- Support if Configuration is missing.
- Support if IP Address is missing.
- Support if Authentication key is not set
- Added domain.configuration missing error dialog

### Fixed

- Fixed total domains blocked display bug.
- Fixed bugs

### Changed

- Optimized code
- Change UI logic
- Size logic
- Config Logic
- Changed Widget version handling

## Ver [0.0.8] - 01-16-2022

### Added

- Added CHANGELOG
- Added COPYRIGHT
- Added widget version

### Changed

- Optimized Code
- Updated README
- Changed project structure
- Changed TOP Blocked to 5 to lighten the interface

## Ver [0.0.7] - 01-15-2022

### Added

- Added Configuration Service
- Added .gitignore

## Ver [0.0.6] - 01-14-2022

### Added

- Added more API calls
- Added more info on other Tiles
- Added more functions to API Service

### Changed

- Changed from multiple DNSs to only 2
- Changed Fluid Tile

## Ver [0.0.5] - 01-13-2022

### Added

- Added Service for PiHole API

## Ver [0.0.4] - 01-12-2022

### Added

- Added LED Tile
- Added Fluid Tile

## Ver [0.0.3] - 01-11-2022

### Added

- initial project
