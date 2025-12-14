# Quick Start Guide - PiHole Widgets

## For End Users

### Installation

1. **Download** the installer for your platform:
   - 🪟 **Windows:** `PiHole-Widgets-{version}-portable.zip`
   - 🍎 **macOS:** `PiHole-Widgets-{version}.pkg`
   - 🐧 **Linux (Debian/Ubuntu):** `pihole-widgets_{version}_amd64.deb`
   - 🐧 **Linux (Fedora/RHEL):** `pihole-widgets-{version}.x86_64.rpm`

2. **Install:**
   - **Windows:** Unzip the portable `.zip` and run `PiHole-Widgets.exe`
   - **macOS:** Double-click the PKG file and follow the wizard
   - **Linux (DEB):** `sudo dpkg -i pihole-widgets_*.deb`
   - **Linux (RPM):** `sudo rpm -i pihole-widgets-*.rpm`

3. **Launch** from:
   - **Windows:** `PiHole-Widgets.exe` (inside the unzipped folder)
   - **macOS:** Applications folder
   - **Linux:** Applications menu

### First-Time Setup

1. **Right-click** on the widget
2. Select **Settings**
3. Enter your Pi-hole details:
   - IP Address (default: 192.168.1.2)
   - Port (default: 80)
   - API Password (see [README](README.md#where-to-find-the-api-token))
4. Click **Apply**
5. The widget will start displaying live Pi-hole data!

### Customization

- **Right-click** the widget > **Settings** > **Widget Configuration**
- Choose your preferred:
  - Widget size (Small, Medium, Large)
  - Layout (Square or Horizontal)
  - Refresh interval

---

## For Developers

### Prerequisites

- Java 25 JDK (set `JAVA_HOME` environment variable)
- Git

### Clone the Repository

```bash
git clone https://github.com/FoKss-LTS/PiHoleWidgets.git
cd PiHoleWidgets
```

### Run Without Building Installer

```bash
# Windows
.\gradlew.bat run

# macOS/Linux
./gradlew run

# With verbose logging
./gradlew run -Ppihole.verbose=true
```

### Build & Test

```bash
# Build the application
./gradlew build

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

### Create Platform Installers

All installers are created using Gradle's `jpackage` task with the `-PinstallerType` parameter:

```bash
# Windows portable (app-image folder with .exe)
./gradlew jpackageImage -PinstallerType=app-image

# Windows portable ZIP (creates a distributable ZIP)
./gradlew portableZip -PinstallerType=app-image

# macOS PKG installer
./gradlew jpackage -PinstallerType=pkg

# macOS DMG (alternative)
./gradlew jpackage -PinstallerType=dmg

# Linux DEB (Debian/Ubuntu)
./gradlew jpackage -PinstallerType=deb

# Linux RPM (Fedora/RHEL)
./gradlew jpackage -PinstallerType=rpm
```

### Common Gradle Commands

| Command | Description |
|---------|-------------|
| `./gradlew build` | Compile and package the application |
| `./gradlew test` | Run unit tests |
| `./gradlew run` | Run the application directly |
| `./gradlew clean` | Clean build outputs |
| `./gradlew jlink` | Create runtime image (without installer) |
| `./gradlew jpackageImage -PinstallerType=app-image` | Windows portable |
| `./gradlew portableZip -PinstallerType=app-image` | Windows portable ZIP |
| `./gradlew jpackage -PinstallerType=pkg` | macOS PKG |
| `./gradlew jpackage -PinstallerType=deb` | Linux DEB |
| `./gradlew jpackage -PinstallerType=rpm` | Linux RPM |

### Project Structure

```
PiHoleWidgets/
├── src/main/java/          # Java source code
│   ├── controllers/        # JavaFX controllers
│   ├── domain/             # Domain models
│   ├── services/           # Business logic
│   └── helpers/            # Utility classes
├── src/main/resources/     # Resources (FXML, icons)
├── build.gradle            # Gradle build configuration
├── .github/workflows/      # CI/CD automation
├── README.md               # Main documentation
├── DISTRIBUTION.md         # Detailed build guide
└── implementation_plan.md  # Development roadmap
```

---

## Troubleshooting

### "Java not found"
**Solution:** Install JDK 25 and set `JAVA_HOME` environment variable

### macOS: "App is damaged"
**Solution:** 
```bash
xattr -cr "/Applications/PiHole Widgets.app"
```

### Linux: "Package conflicts"
**Solution:**
```bash
# Debian/Ubuntu
sudo apt-get install -f

# Fedora/RHEL
sudo dnf install pihole-widgets
```

### Build directory locked (Windows)
**Solution:**
```powershell
./gradlew --stop
```

---

## Additional Resources

- 📖 **Full Documentation:** [README.md](README.md)
- 🔧 **Build Guide:** [DISTRIBUTION.md](DISTRIBUTION.md)
- 🐛 **Issue Tracker:** [GitHub Issues](https://github.com/FoKss-LTS/PiHoleWidgets/issues)
- 📦 **Releases:** [GitHub Releases](https://github.com/FoKss-LTS/PiHoleWidgets/releases)

---

**Version:** 1.5.2  
**Last Updated:** December 2025
