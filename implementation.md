## PiHole Widgets — Implementation Overview

PiHole Widgets is a **cross-platform JavaFX desktop widget** that displays **live statistics from one (and partially two) Pi-hole instances** in a small, always-available dashboard. It targets end users who want Pi-hole visibility without opening the Pi-hole admin UI.

This document is a **self-contained, AI-digestible overview** of what the project does, how it’s built, and where to improve it.

---

## Project Functionality

### What the application does

- **Desktop widget UI**: A borderless JavaFX window (StageStyle `UNDECORATED`) that can be dragged around the desktop and refreshed periodically.
- **System tray integration**: A tray icon + menu to Show/Hide/Exit (and a context menu on the widget itself).
- **Configuration UI**: A settings window that persists connection settings and widget preferences.
- **Pi-hole API integration**: Talks to Pi-hole’s newer `/api/...` endpoints to fetch stats and to toggle DNS blocking.

### What the widget shows (core tiles)

You are an expert Technical Documentation Specialist and Software Architect, highly skilled in analyzing complex software projects and generating comprehensive, AI-digestible documentation.

Your primary task is to analyze the entirety of the provided project context (which you must assume is available or will be provided) and generate the content for a file named `implementation.md`. This file must serve as a high-level, self-contained overview suitable for immediate consumption by another AI agent or a new developer.

The analysis and resulting `implementation.md` content must specifically cover the following three core areas:
1. **Project Functionality:** A clear, concise explanation of what the project does and its primary purpose.
2. **Technology Stack:** A detailed enumeration and description of all core technologies, frameworks, languages, and major dependencies used.
3. **Potential Improvements:** A forward-looking section detailing actionable suggestions for future enhancements, optimizations, or architectural improvements.

Ensure the generated content is structured logically within the `implementation.md` framework. Maintain a professional and objective tone throughout the analysis.

- **Gravity/Ads blocked % (FLUID tile)**  
  - Shows “ads blocked percentage” and a “Gravity: … ago” string.
- **Status counts (STATUS tile)**  
  - Shows processed / blocked / accepted query counters.
  - Displays the “domains being blocked” count and the “last blocked domain”.
- **DNS blocking state + API version (LED tile)**  
  - LED color indicates DNS blocking state: enabled/disabled/mixed/unknown.
  - Clicking the LED “circle” toggles DNS blocking via the Pi-hole API.
  - Shows widget version and Pi-hole API version (best-effort).
- **Top X blocked domains (CUSTOM tile)**  
  - Renders a small table with the top blocked domains and counts.

### Runtime flow (high level)

- **Startup (`controllers.WidgetApplication`)**
  - Loads persisted config via `services.configuration.ConfigurationService`.
  - Loads FXML views (`WidgetContainer.fxml`, `Configuration.fxml`) and wires controllers.
  - Creates and shows the widget Stage and prepares a hidden configuration Stage.
  - Initializes the AWT system tray icon (when supported).
  - If no valid DNS config is present, it opens the configuration window automatically.

- **Widget initialization (`controllers.WidgetController`)**
  - Creates `services.pihole.PiHoleHandler` instance(s) from configured host/port/scheme/token.
  - Initializes TilesFX tiles and schedules periodic refresh tasks.
  - On refresh, fetches JSON from Pi-hole endpoints, parses it with Jackson, and updates the UI via `Platform.runLater`.

- **Configuration persistence (`services.configuration.ConfigurationService`)**
  - Stores settings in a JSON file at:
    - **Windows**: `%USERPROFILE%\Pihole Widget\settings.json`
    - **macOS/Linux**: `$HOME/Pihole Widget/settings.json`
  - Defaults to `pi.hole:80` with `http` scheme when missing.

### Pi-hole API behavior (as implemented)

`services.pihole.PiHoleHandler` communicates over `java.net.http.HttpClient`:

- **Authentication**: `POST /api/auth` with JSON `{ "password": "<token>" }` to obtain a session id (`sid`).
- **Stats**: `GET /api/stats/summary`
- **Recent blocked**: `GET /api/stats/recent_blocked?count=1`
- **Top domains**: `GET /api/stats/top_domains?blocked=true&count=<N>`
- **DNS blocking status**: `GET /api/dns/blocking`
- **Toggle DNS blocking**: `POST /api/dns/blocking` with JSON `{ "blocking": true|false, "timer": <seconds|null> }`

When available, authentication is applied as:
- Query param: `sid=<...>`
- For POST calls: additionally `X-FTL-SID: <sid>`

---

## Technology Stack

### Languages and platform

- **Java**: Java **25** toolchain (see `build.gradle` and `implementation_plan.md`)
- **JPMS (Java modules)**: `module-info.java` defines module `pihole`
- **Target environment**: Cross-platform desktop (Windows/macOS/Linux), with explicit Windows support for packaging

### UI / Desktop

- **JavaFX**: `javafx.controls`, `javafx.fxml`, `javafx.web`, `javafx.swing`
- **FXML**: View definitions in `src/main/resources/controllers/`
- **TilesFX** (`eu.hansolo:tilesfx`): Primary widget “tile” UI library
- **ControlsFX**: JavaFX controls add-ons
- **FormsFX** + **ValidatorFX**: Forms and validation utilities (used by configuration UI)
- **Ikonli JavaFX**: Icon packs for JavaFX
- **BootstrapFX**: Bootstrap-like styling helpers for JavaFX
- **AWT SystemTray**: Tray icon/menu integration (requires `java.desktop`)

### Networking / Data

- **java.net.http.HttpClient**: HTTP transport layer (`helpers.HttpClientUtil`)
- **Jackson** (`jackson-core`, `jackson-databind`): JSON parsing and configuration read/write

### Build, packaging, distribution

- **Gradle**: Build tool (`gradlew`, `build.gradle`)
- **OpenJFX Gradle plugin**: `org.openjfx.javafxplugin`
- **Beryx jlink plugin**: `org.beryx.jlink`
- **jlink/jpackage**: Creates runtime images and native installers (MSI/EXE/PKG/DMG/DEB/RPM)
- **Platform scripts**: `build.ps1`, `build-windows.ps1`, `build-macos.sh`, `build-linux.sh` (see `DISTRIBUTION.md`)

### Testing

- **JUnit Jupiter**: Unit tests for the HTTP client and Pi-hole handler
- **In-memory HTTP server**: `com.sun.net.httpserver.HttpServer` for deterministic tests

---

## Potential Improvements (Actionable)

### Security & configuration handling

- **Encrypt or protect the stored API password/token**: `settings.json` currently stores the authentication token in plaintext; on Windows, consider DPAPI-backed encryption; on macOS Keychain; on Linux Secret Service.
- **Add token validation UX**: Provide a “Test Connection” button in settings and show actionable error messages (bad auth vs unreachable host vs TLS issues).
- **Avoid loading DNS2 as “disabled” in UI**: `Configuration.fxml` currently disables DNS2 settings; if dual Pi-hole support is intended, enable and fully implement it.

### Architecture & maintainability

- **Separate UI from data-fetching logic**: Extract a small “view model” or service layer so `WidgetController` isn’t responsible for scheduling, parsing, and rendering simultaneously.
- **Introduce a typed domain model** for Pi-hole responses instead of ad-hoc `JsonNode` path probing (improves correctness and easier evolution when API changes).
- **Unify refresh interval configuration**: `WidgetConfig` includes update interval fields, but the widget refresh schedule is currently driven by constants; wire the UI settings to actual scheduler intervals.
- **Centralize logging**: Standardize on `java.util.logging` (or a facade) across helpers and services; avoid `System.out` in `HttpClientUtil`.

### Reliability & UX

- **Backoff / retry strategy**: When Pi-hole is unavailable, the widget should degrade gracefully (cached last-known values, exponential backoff, clear “offline” state).
- **Better DNS blocking toggle feedback**: After toggling, show a transient confirmation (or spinner) and reconcile with server state to avoid “mixed/unknown” confusion.
- **Theme support**: Provide explicit theming (dark/light/custom) and persist theme settings; right now some styling is hardcoded in FXML/controller strings.
- **Multi-monitor + DPI correctness**: Ensure tile sizing/layout and tray icon scaling work reliably on high-DPI and multi-monitor setups (Windows especially).

### Build & release hardening (Windows-first)

- **Resolve Windows build directory lock issues**: The plan notes file locking (often OneDrive). Consider moving build output outside synced folders, or documenting a recommended workspace location.
- **Pin JavaFX to a stable GA release**: The build currently references an early-access JavaFX version; migrate to the latest stable JavaFX matching Java 25 when available.
- **Add/restore CI workflow files in-repo**: Documentation references GitHub Actions; ensure `.github/workflows/` is present and kept in sync with `DISTRIBUTION.md`.

---

## Key Files / “Where to look”

- **Entry point**: `src/main/java/controllers/WidgetApplication.java`
- **Widget UI + schedulers**: `src/main/java/controllers/WidgetController.java`
- **Settings UI**: `src/main/java/controllers/ConfigurationController.java`
- **Pi-hole API integration**: `src/main/java/services/pihole/PiHoleHandler.java`
- **Configuration persistence**: `src/main/java/services/configuration/ConfigurationService.java`
- **HTTP utility**: `src/main/java/helpers/HttpClientUtil.java`
- **Build + packaging**: `build.gradle`, `DISTRIBUTION.md`, `QUICKSTART.md`


