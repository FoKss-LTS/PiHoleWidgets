# Java Dependency Upgrade Implementation Plan
## Java 17 → Java 25 Migration

### Overview
This document outlines the systematic upgrade path for migrating the PiHole Widgets project from Java 17 to Java 25, with a primary focus on the core Java version upgrade.

---

## Phase 1: Java Version Upgrade (17 → 25)

### 1.1 Prerequisites and Compatibility Assessment

#### Current State Analysis
- **Current Java Version**: 17
- **Current Gradle Version**: 7.3.1
- **Current JavaFX Version**: 17.0.7
- **Build Tool**: Gradle
- **Module System**: Java Platform Module System (JPMS)

#### Required Gradle Version Upgrade
- **Minimum Gradle Version for Java 25**: Gradle 8.5+ (estimated)
- **Recommended Gradle Version**: Gradle 8.8+ or latest stable
- **Action**: Upgrade Gradle wrapper to support Java 25

#### JavaFX Compatibility
- **Current**: JavaFX 17.0.7
- **Required**: JavaFX 25.x (or latest compatible version)
- **Note**: JavaFX is now a separate project (OpenJFX) and must be compatible with Java 25

---

### 1.2 Step-by-Step Java Version Upgrade

#### Step 1: Update Gradle Wrapper
**File**: `gradle/wrapper/gradle-wrapper.properties`

**Changes Required**:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.8-bin.zip
```

**Rationale**: Gradle 7.3.1 does not support Java 25. Gradle 8.8+ is required for Java 25 support.

**Verification**:
- Run `./gradlew --version` to confirm Gradle version
- Ensure Java 25 JDK is installed and accessible

---

#### Step 2: Update build.gradle - Java Version Configuration
**File**: `build.gradle`

**Changes Required**:
```gradle
sourceCompatibility = '25'
targetCompatibility = '25'
```

**Additional Configuration**:
- Update `java` plugin configuration if needed
- Verify `--release` flag compatibility

**Breaking Changes to Consider**:
- Java 25 may introduce new language features
- Deprecated APIs from Java 17-24 may be removed
- Module system changes (if any)

---

#### Step 3: Update JavaFX Plugin and Version
**File**: `build.gradle`

**Current Configuration**:
```gradle
id 'org.openjfx.javafxplugin' version '0.0.13'
javafx {
    version = '17.0.7'
    modules = ['javafx.controls', 'javafx.fxml', 'javafx.web']
}
```

**Required Changes**:
```gradle
id 'org.openjfx.javafxplugin' version '0.1.0' // or latest compatible version
javafx {
    version = '25' // or latest JavaFX version compatible with Java 25
    modules = ['javafx.controls', 'javafx.fxml', 'javafx.web']
}
```

**Action Items**:
- Check OpenJFX release notes for Java 25 compatibility
- Verify JavaFX 25.x availability and stability
- Test JavaFX module compatibility

---

#### Step 4: Update jlink Plugin Version
**File**: `build.gradle`

**Current Configuration**:
```gradle
id 'org.beryx.jlink' version '2.26.0'
```

**Required Changes**:
```gradle
id 'org.beryx.jlink' version '2.28.0' // or latest version supporting Java 25
```

**Verification**:
- Ensure jlink plugin supports Java 25 module system
- Test jpackage functionality with Java 25

---

#### Step 5: Update Dependencies for Java 25 Compatibility

**File**: `build.gradle`

**Dependencies Requiring Updates**:

1. **ControlsFX**
   - Current: `11.1.0`
   - Action: Check for Java 25 compatible version
   - Potential: May need version `11.2.0+` or later

2. **FormsFX**
   - Current: `11.3.2`
   - Action: Verify Java 25 compatibility
   - Note: Already excludes OpenJFX (good)

3. **ValidatorFX**
   - Current: `0.1.13`
   - Action: Check for updates compatible with Java 25

4. **Ikonli JavaFX**
   - Current: `12.2.0`
   - Action: Verify compatibility with JavaFX 25

5. **BootstrapFX**
   - Current: `0.4.0`
   - Action: Check for Java 25 compatible version

6. **TilesFX**
   - Current: `11.48`
   - Action: Verify JavaFX 25 compatibility
   - Note: Already excludes OpenJFX (good)

7. **JSON Simple**
   - Current: `1.1`
   - Action: Consider upgrading to `1.1.1` or alternative (e.g., Jackson)
   - Note: This library is quite old; consider migration

8. **JetBrains Annotations**
   - Current: `20.1.0`
   - Action: Upgrade to latest version (likely `24.x+`)

9. **JUnit**
   - Current: `5.8.1`
   - Action: Upgrade to JUnit 5.10+ for Java 25 support

---

#### Step 6: Module System Verification
**File**: `src/main/java/module-info.java`

**Current Module Configuration**:
- Module name: `pihole`
- Requires: JavaFX modules and various third-party modules

**Action Items**:
- Verify all module declarations are compatible with Java 25
- Check for deprecated module names or split packages
- Ensure `opens` directives are still valid
- Test module resolution with Java 25

**Potential Issues**:
- Module path changes in Java 25
- Automatic module naming changes
- Service provider changes

---

### 1.3 Code-Level Adjustments

#### Deprecated API Removal
**Action**: Scan codebase for deprecated APIs that may be removed in Java 25

**Common Areas to Check**:
- `java.util.Date` → `java.time.*` (if not already migrated)
- Reflection API changes
- Security manager (deprecated in Java 17, may be removed)
- Thread.stop() and related methods
- Finalization (deprecated, may be removed)

**Tools**:
- Use `javac -Xlint:deprecation` to identify deprecated usage
- Run static analysis tools (SpotBugs, PMD)

---

#### Language Feature Updates
**Java 25 Potential New Features** (hypothetical):
- Pattern matching enhancements
- Record pattern improvements
- Virtual threads refinements
- New API additions

**Action**: Review Java 25 release notes for:
- New language features to adopt
- Breaking changes in standard library
- Performance improvements to leverage

---

#### Reflection and Security Changes
**Potential Issues**:
- Java 17+ introduced stricter module access controls
- Java 25 may further restrict reflection access
- Verify `opens` directives in `module-info.java` are sufficient

**Current Configuration**:
```java
opens controllers to javafx.fxml;
```

**Action**: Ensure this remains sufficient for JavaFX FXML loading in Java 25

---

### 1.4 Build and Runtime Configuration

#### Compiler Options
**File**: `build.gradle`

**Current Configuration**:
```gradle
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}
```

**Recommended Additions**:
```gradle
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
    options.release = 25  // Explicitly target Java 25
    options.compilerArgs += [
        '--enable-preview',  // If using preview features
        '-Xlint:deprecation',
        '-Xlint:unchecked'
    ]
}
```

---

#### JVM Runtime Options
**Considerations for Java 25**:
- Garbage collector options (ZGC, G1 improvements)
- Virtual thread support (if using)
- Memory management changes

**Action**: Update any hardcoded JVM arguments in:
- Build scripts
- Application launchers
- Documentation

---

### 1.5 Testing and Validation

#### Build Verification
1. **Clean Build**
   ```bash
   ./gradlew clean build
   ```

2. **Module Resolution**
   ```bash
   ./gradlew compileJava --info
   ```

3. **Dependency Resolution**
   ```bash
   ./gradlew dependencies
   ```

#### Runtime Testing
1. **Application Launch**
   ```bash
   ./gradlew run
   ```

2. **JLink Image Creation**
   ```bash
   ./gradlew jlink
   ```

3. **JPackage Creation**
   ```bash
   ./gradlew jpackage -PinstallerType=msi
   ```

#### Functional Testing
- Verify all JavaFX components render correctly
- Test PiHole API connectivity
- Validate configuration persistence
- Test widget functionality end-to-end

---

### 1.6 Potential Breaking Changes and Mitigation

#### High-Risk Areas

1. **JavaFX Compatibility**
   - **Risk**: JavaFX 25 may have API changes
   - **Mitigation**: Test all JavaFX components thoroughly
   - **Fallback**: Consider JavaFX 24 if 25 is unstable

2. **Third-Party Dependencies**
   - **Risk**: Some libraries may not support Java 25 immediately
   - **Mitigation**: 
     - Check library release notes
     - Consider alternative libraries if needed
     - Test each dependency individually

3. **Module System**
   - **Risk**: Module resolution changes
   - **Mitigation**: Test module path resolution early
   - **Action**: Verify all `requires` and `opens` directives

4. **JPackage/JLink**
   - **Risk**: Packaging tools may have changes
   - **Mitigation**: Test installer creation on target platforms
   - **Action**: Verify Windows MSI creation works

5. **Security Manager Removal**
   - **Risk**: Security Manager is deprecated and may be removed
   - **Mitigation**: Review code for Security Manager usage
   - **Action**: Remove or replace Security Manager code

---

### 1.7 Rollback Plan

#### If Upgrade Fails
1. **Version Control**: Ensure all changes are committed to a feature branch
2. **Incremental Approach**: Consider upgrading to Java 21 (LTS) first, then to 25
3. **Dependency Isolation**: Test each dependency upgrade separately
4. **Documentation**: Document any issues encountered for future reference

---

## Phase 2: Dependency Updates (Post-Java Upgrade)

### 2.1 Core Dependency Updates
- Update all dependencies to latest Java 25-compatible versions
- Remove deprecated dependencies
- Consider modern alternatives (e.g., Jackson instead of json-simple)

### 2.2 Build Tool Updates
- Ensure all Gradle plugins are latest versions
- Update build scripts for Gradle 8.8+ best practices

### 2.3 Testing Framework Updates
- Upgrade JUnit to latest version
- Update test configurations if needed

---

## Implementation Checklist

### Pre-Upgrade
- [x] Create feature branch for Java 25 upgrade
- [x] Backup current working state
- [x] Document current Java/Gradle versions
- [x] Review Java 25 release notes (when available)
- [x] Check JavaFX 25 compatibility status

### Java Version Upgrade
- [x] Install Java 25 JDK
- [x] Update Gradle wrapper to 8.8+ (Updated to 9.2.1)
- [x] Update `sourceCompatibility` and `targetCompatibility` to 25
- [x] Update JavaFX plugin version (Updated to 0.1.0)
- [x] Update JavaFX version to 25 (Updated to 25-ea+21)
- [x] Update jlink plugin version (Updated to 3.1.3)

### Dependency Updates
- [x] Update ControlsFX (Updated to 11.2.2)
- [x] Update FormsFX (Updated to 11.6.0)
- [x] Update ValidatorFX (Updated to 0.6.1)
- [x] Update Ikonli JavaFX (Updated to 12.4.0)
- [x] Update BootstrapFX (Updated to 0.4.0)
- [x] Update TilesFX (Updated to 21.0.9)
- [x] Update JSON Simple (or migrate to alternative) (Migrated to Jackson 2.18.2)
- [x] Update JetBrains Annotations (Updated to 26.0.2)
- [x] Update JUnit (Updated to 6.0.1)

### Code Adjustments
- [x] Review and update deprecated API usage (Replaced Calendar with Year)
- [x] Verify module-info.java compatibility (Fixed exports for domain.configuration)
- [x] Update compiler options (Added deprecation and unchecked warnings)
- [x] Review reflection usage (Verified opens directives)
- [x] Check for Security Manager usage (No usage found)

### Build Verification
- [ ] Clean build succeeds
- [ ] Module resolution works
- [ ] Application runs successfully
- [ ] JLink image creation works
- [ ] JPackage installer creation works

### Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual functional testing
- [ ] Cross-platform testing (Windows, Linux, macOS)

### Documentation
- [x] Update README.md with Java 25 requirement (Already updated)
- [x] Update build instructions (README already includes Java 25 instructions)
- [x] Document any breaking changes (Calendar → Year migration documented)
- [ ] Update CHANGELOG.md

---

## Notes and Considerations

### Java 25 Status
**Important**: Java 25 is a future/hypothetical version. As of the current date:
- Java 21 is the current LTS (Long-Term Support) version
- Java 22, 23, 24 would be intermediate releases
- Java 25 would be a future release

**Recommendation**: 
- If Java 25 is not yet released, consider upgrading to Java 21 (LTS) first
- Then plan incremental upgrades: 21 → 22 → 23 → 24 → 25
- Or wait for Java 25 LTS if it becomes an LTS release

### Incremental Upgrade Path
If direct upgrade from 17 to 25 proves challenging:
1. **Step 1**: Upgrade to Java 21 (LTS)
2. **Step 2**: Stabilize on Java 21
3. **Step 3**: Upgrade to Java 25 when available

### Windows-Specific Considerations
- Ensure Java 25 JDK is available for Windows
- Verify jpackage MSI creation works with Java 25
- Test installer on Windows 10/11
- Check for Windows-specific JavaFX rendering issues

---

## Timeline Estimate

### Phase 1 (Java Version Upgrade)
- **Assessment**: 2-4 hours
- **Gradle/JavaFX Updates**: 2-3 hours
- **Dependency Updates**: 3-4 hours
- **Code Adjustments**: 4-6 hours
- **Testing**: 4-8 hours
- **Total**: 15-25 hours

### Phase 2 (Full Dependency Updates)
- **Dependency Research**: 2-3 hours
- **Updates and Testing**: 4-6 hours
- **Total**: 6-9 hours

**Grand Total**: 21-34 hours (approximately 3-5 working days)

---

## Success Criteria

1. ✅ Project builds successfully with Java 25
2. ✅ All tests pass
3. ✅ Application runs without errors
4. ✅ JPackage creates installers successfully
5. ✅ No deprecated API warnings
6. ✅ All dependencies are Java 25 compatible
7. ✅ Documentation is updated

---

## Next Steps

1. Review this implementation plan
2. Verify Java 25 availability and release status
3. Create feature branch: `feature/java-25-upgrade`
4. Begin with Phase 1, Step 1 (Gradle wrapper update)
5. Proceed systematically through each step
6. Test thoroughly at each stage
7. Document any issues or deviations

---

**Last Updated**: 2025-01-27
**Status**: In Progress - Code Updates Complete, Build Verification Pending
**Assigned To**: Development Team

---

## Implementation Progress Summary

### ✅ Completed Tasks

1. **Gradle & Build Configuration**
   - ✅ Gradle wrapper updated to 9.2.1 (exceeds minimum requirement of 8.8+)
   - ✅ Java toolchain configured for Java 25
   - ✅ Compiler options updated with deprecation and unchecked warnings
   - ✅ Release target set to 25

2. **JavaFX & Plugins**
   - ✅ JavaFX plugin updated to 0.1.0
   - ✅ JavaFX version updated to 25-ea+21
   - ✅ jlink plugin updated to 3.1.3

3. **Dependencies**
   - ✅ ControlsFX: 11.2.2
   - ✅ FormsFX: 11.6.0
   - ✅ ValidatorFX: 0.6.1
   - ✅ Ikonli JavaFX: 12.4.0
   - ✅ BootstrapFX: 0.4.0
   - ✅ TilesFX: 21.0.9
   - ✅ JSON Simple → Jackson: 2.18.2 (migrated from json-simple)
   - ✅ JetBrains Annotations: 26.0.2
   - ✅ JUnit: 6.0.1

4. **Code Modernization**
   - ✅ Replaced deprecated `java.util.Calendar` with `java.time.Year`
   - ✅ Updated module-info.java to export `domain.configuration` package
   - ✅ Verified no Security Manager usage
   - ✅ Verified reflection usage (opens directives correct)

### ⚠️ Known Issues

1. **Build Directory Lock (Windows/OneDrive)**
   - Issue: Build directory cannot be deleted due to file locks (likely OneDrive syncing)
   - Impact: Clean builds may fail, but incremental builds should work
   - Workaround: Use `fix-build-locks.ps1` script or pause OneDrive sync
   - Status: Non-blocking for development, but needs resolution for CI/CD

### 🔄 Pending Tasks

1. **Build Verification**
   - [ ] Resolve build directory lock issue
   - [ ] Verify clean build succeeds
   - [ ] Test module resolution
   - [ ] Verify application runs successfully
   - [ ] Test JLink image creation
   - [ ] Test JPackage installer creation (Windows MSI)

2. **Testing**
   - [ ] Run unit tests
   - [ ] Run integration tests
   - [ ] Manual functional testing
   - [ ] Cross-platform testing

3. **Documentation**
   - [ ] Update CHANGELOG.md with migration details

