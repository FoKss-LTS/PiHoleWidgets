

# <ins>Table of contents</ins>
- [Overview](#overview)
  * [Features](#features)
  * [Supported Platforms](#supported-platforms)
  * [Upcoming features](#upcoming-features)
- [Support](#donation)
- [Installation](#installation)
    * [Pre-Requisites](#pre-requisites)
    * [Installation Options](#installation-options)
    * [Configuration](#configuration)
      * [Where to find the API TOKEN](#where-to-find-the-api-token)
      * [Personalize your Widget](#personalize-your-widget)
    * [Running the widget](#running-the-widget)
- [Testing](#testing) 
- [Distribution](#distribution)
- [Credits](#credits)
- [License](#licence)

# <ins>Overview</ins>
## Pi-Hole DNS Widget for Desktop
PiHole Widgets is a project I needed to make for my personal needs, and I decided to share it with you.

It is developed with JavaFX, and uses the framework TilesFX.

##### Screen Shots
###### <ins>Square</ins>
![img_5.png](Readme_imgs/img_5.png)

###### <ins>Horizontal</ins>
![img_7.png](Readme_imgs/img_7.png)

### <ins>Features</ins>

- Live stats from Pi-hole servers.
- Modern, customizable widgets.
- **Cross-platform support:** Windows, macOS, and Linux.
- **Easy distribution:** Portable on Windows + native packages on macOS/Linux (PKG, DEB, RPM).
- **No Java installation required:** Java runtime bundled with all packages.
- Great summary of your Pi-hole DNS in one place.
- Show the last blocked domain.
- Shows last time gravity was updated.
- Change visual aspects of the widget.
- Adjustable widget size to fit your needs.
- Simple configuration interface.

### <ins>Supported Platforms</ins>

| Platform | Installer Type | Minimum Version | Status |
|----------|---------------|----------------|--------|
| **Windows** | Portable EXE (app-image) | Windows 10 (64-bit) | ✅ Fully Supported |
| **macOS** | PKG | macOS 10.15 (Catalina) | ✅ Fully Supported |
| **Linux (Debian/Ubuntu)** | DEB | Ubuntu 20.04+ | ✅ Fully Supported |
| **Linux (Fedora/RHEL)** | RPM | Fedora 35+ | ✅ Fully Supported |

*All installers include bundled Java runtime - no separate installation required!*

### <ins>Upcoming features</ins>

- Support for 2 Pi-Holes.
- Beautiful Themes.
- Hide to tray option.
- Enable/Disable Pi-hole from widget.


# <ins>Donation</ins>
All donations are welcome and any amount of money will help me to maintain this project :)
<p align="left">  
  <a href="https://paypal.me/foxinflames"><img alt="Donate using Paypal" src="https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif"></a>
</p>


# <ins>Installation</ins>

### Pre-Requisites

**For End Users (using installers):**
- None! Java runtime is bundled with all installers.
- Last version of Pi-hole running on your network.

**For Developers (building from source):**
- Java 25 JDK (set `JAVA_HOME` to your JDK 25 installation).
- Git (for cloning the repository).
- Platform-specific build tools (see [DISTRIBUTION.md](DISTRIBUTION.md) for details).

### Configuration
The widget automatically sets its configuration to the default pi-hole local address if that doesn't work , open the configuration interface by right clicking on the widget then click on Settings.

![img_8.png](Readme_imgs/img_8.png)

Then input your controllers IP Address/Port and API Token, and click Apply.

###### Where to find the API TOKEN

![img.png](Readme_imgs/img.png)

![img_3.png](Readme_imgs/img_3.png)

![img_4.png](Readme_imgs/img_4.png)

###### Personalize your widget

You can set your Widget size or layout by accessing the Widget configuration panel in Settings (Restart needed).

![img_10.png](Readme_imgs/img_10.png)

### Installation Options

#### Option 1: Download Pre-built Installers (Recommended)

Download the latest installer for your platform from the [Releases page](https://github.com/foxy999/PiHoleWidgets/releases):

- **Windows:** Download the Windows portable `.zip`, unzip it, and run `PiHole-Widgets.exe`
- **macOS:** Download and run the `.pkg` installer
- **Linux (Debian/Ubuntu):** Download the `.deb` file and install with:
  ```bash
  sudo dpkg -i pihole-widgets_*.deb
  ```
- **Linux (Fedora/RHEL):** Download the `.rpm` file and install with:
  ```bash
  sudo rpm -i pihole-widgets-*.rpm
  ```

All packages include the bundled Java runtime - no separate Java installation needed!

#### Option 2: Build from Source

**Quick Build (Auto-detects your OS):**
```bash
# Windows PowerShell
.\build.ps1

# macOS/Linux
chmod +x build.sh && ./build.sh
```

**Platform-Specific Builds:**
```bash
# Windows
.\build-windows.ps1

# macOS
./build-macos.sh

# Linux (DEB for Debian/Ubuntu)
./build-linux.sh --type deb

# Linux (RPM for Fedora/RHEL)
./build-linux.sh --type rpm

# Linux (Both DEB and RPM)
./build-linux.sh --type both
```

**Run without Building Installer:**
```bash
# Windows
.\gradlew.bat run

# macOS/Linux
./gradlew run
```

For detailed build instructions, see [DISTRIBUTION.md](DISTRIBUTION.md).

### Running the widget

After installation, launch PiHole Widgets from:
- **Windows:** Start Menu > PiHole Widgets
- **macOS:** Applications folder
- **Linux:** Applications menu (under Network or Office)

If you want to close the widget, right-click on it and select "Close" from the context menu. You can also update manually from the same menu.

![img_9.png](Readme_imgs/img_9.png)

# <ins>Testing</ins>

App has been tested on:
- **Windows 10 & 11** (64-bit)
- **macOS** (Catalina and later)
- **Linux Mint, Ubuntu, and Fedora**

Automated builds and testing are performed via GitHub Actions on all supported platforms.

If you find any bugs or want to suggest new features please go to: https://github.com/foxy999/PiHoleWidgets/issues

---

# <ins>Distribution</ins>

## For Developers & Distributors

This project includes comprehensive cross-platform build support:

**📚 Documentation:**
- **[QUICKSTART.md](QUICKSTART.md)** - Quick reference for users and developers
- **[DISTRIBUTION.md](DISTRIBUTION.md)** - Detailed build and distribution guide
- **[DISTRIBUTION_SUMMARY.md](DISTRIBUTION_SUMMARY.md)** - Implementation summary

**🔧 Build Scripts:**
- `build.ps1` / `build.sh` - Universal build (auto-detects OS)
- `build-windows.ps1` / `build-windows.bat` - Windows portable (app-image EXE)
- `build-macos.sh` - macOS PKG
- `build-linux.sh` - Linux DEB/RPM

**🚀 CI/CD:**
- GitHub Actions automatically builds all platforms
- Tagged releases create installers for Windows, macOS, and Linux
- See `.github/workflows/build.yml`

---


# <ins>Credits</ins>
<div>
Shoutout to :

- <a href="https://github.com/HanSolo/tilesfx" title="hans0l0">hans0l0</a> for the project TilesFX.
 
- <a href="https://github.com/afsalashyana" title="afsalashyana">afsalashyana</a> for the helpful Covid Widget and JavaFX Tutorials.

</div>

# <ins>Licence</ins>
Copyright (C) 2022.  Reda ELFARISSI aka foxy999


This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.         

Go back to : [Table of Content](#table-of-contents)