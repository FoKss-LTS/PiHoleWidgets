# Dependency Update Management

This project uses the [Gradle Versions Plugin](https://github.com/ben-manes/gradle-versions-plugin) to help keep dependencies up to date.

## Running Dependency Updates Check

To check for available dependency updates, run:

```bash
./gradlew dependencyUpdates --no-parallel
```

> **Note**: The `--no-parallel` flag is required when using Gradle 9.0+ due to a known issue with the plugin.

## Understanding the Report

The plugin generates two types of reports in `build/reports/dependencyUpdates/`:

1. **dependency-updates-report.txt** - Plain text report (for terminal viewing)
2. **dependency-updates-report.html** - HTML report (for browser viewing)

### Report Sections

- **Current dependencies** - Dependencies that are up to date
- **Dependencies with updates** - Dependencies that have newer stable versions available
- **Gradle updates** - Available Gradle version updates
- **Unresolved dependencies** - Dependencies that failed to resolve

## Version Filtering

The plugin is configured to **only show stable releases**. The following unstable version patterns are automatically filtered out:

- Alpha versions (e.g., `1.0.0-alpha`)
- Beta versions (e.g., `1.0.0-beta`)
- Release candidates (e.g., `1.0.0-RC1`)
- Milestone versions (e.g., `1.0.0-M1`)
- Snapshot versions (e.g., `1.0.0-SNAPSHOT`)

This ensures you only receive recommendations for production-ready dependency versions.

## Updating Dependencies

After reviewing the report:

1. Open `build.gradle`
2. Update the version numbers for the dependencies you want to upgrade
3. Run `./gradlew build` to verify the build still works
4. Run `./gradlew test` to verify all tests pass

## Automation

Consider setting up a reminder to run this check:

- **Weekly**: For active development projects
- **Monthly**: For maintenance-mode projects
- **Before releases**: Always check before cutting a new release

## Gradle Updates

The plugin also checks for Gradle updates. To update Gradle itself:

```bash
./gradlew wrapper --gradle-version <new-version>
```

For example:
```bash
./gradlew wrapper --gradle-version 9.3.0
```

## Additional Options

For more verbose output:
```bash
./gradlew dependencyUpdates --no-parallel --info
```

To force refresh of dependency cache:
```bash
./gradlew dependencyUpdates --no-parallel --refresh-dependencies
```

## Configuration

The plugin configuration can be found at the bottom of `build.gradle`. You can customize:

- Output format (plain, html, json, xml)
- Output directory
- Gradle release channel (current, release-candidate, nightly)
- Version rejection rules

## Troubleshooting

### Issue: Task fails with NoSuchMethodError
**Solution**: Ensure you're using plugin version 0.53.0 or later with Gradle 9+

### Issue: Takes too long to run
**Solution**: Check specific configurations only:
```bash
./gradlew dependencyUpdates --no-parallel -DdependencyUpdates.revision=release
```

### Issue: Shows too many updates
**Solution**: The version filtering is configured to only show stable releases. If you're still seeing too many updates, you can be more selective about which dependencies to update based on your project's needs.

## Resources

- [Plugin Documentation](https://github.com/ben-manes/gradle-versions-plugin)
- [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.github.ben-manes.versions)
