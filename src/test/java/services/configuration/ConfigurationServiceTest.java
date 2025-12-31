package services.configuration;

import domain.configuration.DnsBlockerConfig;
import domain.configuration.DnsBlockerType;
import domain.configuration.WidgetConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigurationService.
 * Note: These tests use the actual user home directory for configuration files.
 */
class ConfigurationServiceTest {

    private ConfigurationService configService;
    private Path configFilePath;
    private boolean configFileExisted;

    @BeforeEach
    void setUp() {
        configService = new ConfigurationService();
        // ConfigurationService uses System.getProperty("user.home") which is set at
        // class load time
        // So we work with the actual user home directory
        String home = System.getProperty("user.home");
        configFilePath = Path.of(home, "DNSBlocker Widget", "settings.json");
        configFileExisted = Files.exists(configFilePath);

        // Backup existing config if it exists
        if (configFileExisted) {
            try {
                Files.move(configFilePath, Path.of(home, "DNSBlocker Widget", "settings.json.backup"));
            } catch (IOException e) {
                // Ignore backup failures
            }
        }
    }

    @AfterEach
    void tearDown() {
        // Restore original config if it existed
        if (configFileExisted) {
            try {
                Path backupPath = Path.of(System.getProperty("user.home"), "DNSBlocker Widget", "settings.json.backup");
                if (Files.exists(backupPath)) {
                    Files.move(backupPath, configFilePath);
                }
            } catch (IOException e) {
                // Ignore restore failures
            }
        } else {
            // Clean up test config file
            try {
                if (Files.exists(configFilePath)) {
                    Files.delete(configFilePath);
                }
                // Try to remove directory if empty
                Path configDir = configFilePath.getParent();
                if (Files.exists(configDir) && Files.list(configDir).findAny().isEmpty()) {
                    Files.delete(configDir);
                }
            } catch (IOException e) {
                // Ignore cleanup failures
            }
        }
    }

    @Test
    void testReadConfigurationCreatesFileIfNotExists() {
        // Delete file if it exists
        try {
            if (Files.exists(configFilePath)) {
                Files.delete(configFilePath);
            }
        } catch (IOException e) {
            // Ignore
        }

        assertFalse(Files.exists(configFilePath));

        configService.readConfiguration();

        assertTrue(Files.exists(configFilePath));
    }

    @Test
    void testReadConfigurationWithValidJson() throws IOException {
        // Create a valid config file
        Files.createDirectories(configFilePath.getParent());
        String json = """
                {
                  "DNS1": {
                    "Scheme": "https",
                    "IP": "192.168.1.1",
                    "Port": 443,
                    "Password": "token123"
                  },
                  "Widget": {
                    "Size": "Large",
                    "Layout": "Horizontal",
                    "Theme": "Light",
                    "TopX": 3,
                    "UpdateStatusSec": 10,
                    "UpdateFluidSec": 20,
                    "UpdateActiveSec": 30,
                    "UpdateTopXSec": 40
                  }
                }
                """;
        Files.writeString(configFilePath, json);

        configService.readConfiguration();

        DnsBlockerConfig dns1 = configService.getConfigDNS1();
        assertNotNull(dns1);
        assertEquals("192.168.1.1", dns1.getIPAddress());
        assertEquals(443, dns1.getPort());
        assertEquals("https", dns1.getScheme());
        assertEquals("token123", dns1.password());

        WidgetConfig widget = configService.getWidgetConfig();
        assertNotNull(widget);
        assertEquals("Large", widget.getSize());
        assertEquals("Horizontal", widget.getLayout());
        assertEquals("Light", widget.getTheme());
        assertEquals(10, widget.getUpdate_status_sec());
        assertEquals(20, widget.getUpdate_fluid_sec());
        assertEquals(30, widget.getUpdate_active_sec());
        assertEquals(40, widget.getUpdate_topx_sec());
        assertEquals(3, widget.topX());
    }

    @Test
    void testReadConfigurationWithMissingDns1() throws IOException {
        Files.createDirectories(configFilePath.getParent());
        String json = """
                {
                  "Widget": {
                    "Size": "Medium",
                    "Layout": "Square",
                    "Theme": "Dark"
                  }
                }
                """;
        Files.writeString(configFilePath, json);

        configService.readConfiguration();

        assertNull(configService.getConfigDNS1());
        assertNotNull(configService.getWidgetConfig());
    }

    @Test
    void testReadConfigurationWithMissingWidget() throws IOException {
        Files.createDirectories(configFilePath.getParent());
        String json = """
                {
                  "DNS1": {
                    "Scheme": "http",
                    "IP": "192.168.1.1",
                    "Port": 80,
                    "Password": "token"
                  }
                }
                """;
        Files.writeString(configFilePath, json);

        configService.readConfiguration();

        assertNotNull(configService.getConfigDNS1());
        assertNotNull(configService.getWidgetConfig());
        // Should use defaults
        assertEquals(WidgetConfig.DEFAULT_SIZE, configService.getWidgetConfig().getSize());
    }

    @Test
    void testReadConfigurationWithInvalidJson() throws IOException {
        Files.createDirectories(configFilePath.getParent());
        Files.writeString(configFilePath, "invalid json");

        // Should not throw exception, but configs should be null or default
        assertDoesNotThrow(() -> configService.readConfiguration());
    }

    @Test
    void testWriteConfigFile() {
        boolean result = configService.writeConfigFile(
                DnsBlockerType.PIHOLE, "https", "192.168.1.1", 443, "", "token1",
                DnsBlockerType.PIHOLE, "http", "192.168.1.2", 80, "", "token2",
                "Large", "Horizontal", "Light",
                true, true, true,
                2,
                10, 20, 30, 40);

        assertTrue(result);
        assertTrue(Files.exists(configFilePath));
    }

    @Test
    void testWriteConfigFileCreatesDirectory() {
        // Delete directory if it exists
        try {
            if (Files.exists(configFilePath)) {
                Files.delete(configFilePath);
            }
            if (Files.exists(configFilePath.getParent())) {
                // Try to delete directory, but it might not be empty
                try {
                    Files.delete(configFilePath.getParent());
                } catch (IOException e) {
                    // Directory not empty or other issue - that's okay for this test
                }
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }

        // Note: The directory might still exist from previous tests, so we just verify
        // that writeConfigFile creates it if needed and creates the file
        boolean result = configService.writeConfigFile(
                DnsBlockerType.PIHOLE, "http", "192.168.1.1", 80, "", "token",
                DnsBlockerType.PIHOLE, "http", "", 80, "", "",
                "Medium", "Square", "Dark",
                true, true, true,
                2,
                5, 15, 60, 5);

        assertTrue(result);
        // Verify the file was created (which implies directory was created if needed)
        assertTrue(Files.exists(configFilePath));
        assertTrue(Files.exists(configFilePath.getParent()));
    }

    @Test
    void testWriteAndReadConfiguration() {
        // Write configuration
        configService.writeConfigFile(
                DnsBlockerType.PIHOLE, "https", "192.168.1.1", 443, "", "mytoken",
                DnsBlockerType.PIHOLE, "http", "", 80, "", "",
                "Large", "Horizontal", "Light",
                true, true, true,
                4,
                10, 20, 30, 40);

        // Create new service instance and read
        ConfigurationService newService = new ConfigurationService();
        newService.readConfiguration();

        DnsBlockerConfig dns1 = newService.getConfigDNS1();
        assertNotNull(dns1);
        assertEquals("192.168.1.1", dns1.getIPAddress());
        assertEquals(443, dns1.getPort());
        assertEquals("https", dns1.getScheme());
        assertEquals("mytoken", dns1.password());

        WidgetConfig widget = newService.getWidgetConfig();
        assertNotNull(widget);
        assertEquals("Large", widget.getSize());
        assertEquals("Horizontal", widget.getLayout());
        assertEquals("Light", widget.getTheme());
        assertEquals(10, widget.getUpdate_status_sec());
        assertEquals(20, widget.getUpdate_fluid_sec());
        assertEquals(30, widget.getUpdate_active_sec());
        assertEquals(40, widget.getUpdate_topx_sec());
        assertEquals(4, widget.topX());
    }

    @Test
    void testSaveEmptyConfiguration() {
        boolean result = configService.saveEmptyConfiguration();

        assertTrue(result);
        assertTrue(Files.exists(configFilePath));
    }

    @Test
    void testGetConfigDNS2ReturnsNull() {
        // DNS2 support is disabled, should always return null
        assertNull(configService.getConfigDNS2());

        configService.readConfiguration();

        assertNull(configService.getConfigDNS2());
    }

    @Test
    void testWriteConfigFileWithNullTheme() {
        boolean result = configService.writeConfigFile(
                DnsBlockerType.PIHOLE, "http", "192.168.1.1", 80, "", "token",
                DnsBlockerType.PIHOLE, "http", "", 80, "", "",
                "Medium", "Square", null,
                true, true, true,
                2,
                5, 15, 60, 5);

        assertTrue(result);

        // Read back and verify default theme is used
        ConfigurationService newService = new ConfigurationService();
        newService.readConfiguration();
        assertEquals(WidgetConfig.DEFAULT_THEME, newService.getWidgetConfig().getTheme());
    }

    // ==================== Self-Healing Tests ====================

    @Nested
    @DisplayName("Self-Healing for Corrupt Configuration")
    class SelfHealingTests {

        @Test
        @DisplayName("Should self-heal from corrupt JSON and create backup")
        void shouldSelfHealFromCorruptJson() throws IOException {
            // Create a corrupt JSON file
            Files.createDirectories(configFilePath.getParent());
            Files.writeString(configFilePath, "{ this is not valid JSON !!!");

            // Read should trigger self-healing
            configService.readConfiguration();

            // Verify that defaults are loaded
            WidgetConfig widget = configService.getWidgetConfig();
            assertNotNull(widget, "Widget config should not be null after self-heal");
            assertEquals(WidgetConfig.DEFAULT_SIZE, widget.getSize());
            assertEquals(WidgetConfig.DEFAULT_LAYOUT, widget.getLayout());
            assertEquals(WidgetConfig.DEFAULT_THEME, widget.getTheme());

            // Verify backup file was created
            List<Path> backupFiles = findBackupFiles();
            assertTrue(backupFiles.size() >= 1, "Should create at least one backup file");

            // Clean up backup files
            for (Path backup : backupFiles) {
                Files.deleteIfExists(backup);
            }
        }

        @Test
        @DisplayName("Should self-heal from truncated JSON file")
        void shouldSelfHealFromTruncatedJson() throws IOException {
            Files.createDirectories(configFilePath.getParent());
            // Truncated JSON - missing closing braces
            Files.writeString(configFilePath, "{\"DNS1\": {\"IP\": \"192.168.1.1\"");

            configService.readConfiguration();

            WidgetConfig widget = configService.getWidgetConfig();
            assertNotNull(widget, "Should have default widget config after self-heal");

            // Clean up any backup files
            cleanupBackupFiles();
        }

        @Test
        @DisplayName("Should self-heal from empty file")
        void shouldSelfHealFromEmptyFile() throws IOException {
            Files.createDirectories(configFilePath.getParent());
            Files.writeString(configFilePath, "");

            configService.readConfiguration();

            WidgetConfig widget = configService.getWidgetConfig();
            assertNotNull(widget, "Should have default widget config after self-heal");

            cleanupBackupFiles();
        }

        @Test
        @DisplayName("Should self-heal from binary garbage")
        void shouldSelfHealFromBinaryGarbage() throws IOException {
            Files.createDirectories(configFilePath.getParent());
            byte[] garbage = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, 0x03};
            Files.write(configFilePath, garbage);

            configService.readConfiguration();

            WidgetConfig widget = configService.getWidgetConfig();
            assertNotNull(widget, "Should have default widget config after self-heal");

            cleanupBackupFiles();
        }

        @Test
        @DisplayName("Backup file should contain original corrupt content")
        void backupFileShouldContainOriginalContent() throws IOException {
            // Clean up any existing backups first to ensure isolation
            cleanupBackupFiles();
            
            String corruptContent = "{ corrupt content 12345 }";
            Files.createDirectories(configFilePath.getParent());
            Files.writeString(configFilePath, corruptContent);

            configService.readConfiguration();

            List<Path> backupFiles = findBackupFiles();
            assertFalse(backupFiles.isEmpty(), "Should create backup file");

            // Find the most recent backup (highest timestamp)
            Path mostRecentBackup = backupFiles.stream()
                    .max((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                    .orElse(null);
            assertNotNull(mostRecentBackup, "Should find a backup file");

            // Verify the backup contains the original corrupt content
            String backupContent = Files.readString(mostRecentBackup);
            assertEquals(corruptContent, backupContent, "Backup should contain original corrupt content");

            cleanupBackupFiles();
        }

        private List<Path> findBackupFiles() throws IOException {
            List<Path> backups = new ArrayList<>();
            Path parent = configFilePath.getParent();
            if (Files.exists(parent)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, "settings.json.corrupt.*")) {
                    for (Path path : stream) {
                        backups.add(path);
                    }
                }
            }
            return backups;
        }

        private void cleanupBackupFiles() throws IOException {
            for (Path backup : findBackupFiles()) {
                Files.deleteIfExists(backup);
            }
        }
    }

    // ==================== Atomic Write Tests ====================

    @Nested
    @DisplayName("Atomic Write Operations")
    class AtomicWriteTests {

        @Test
        @DisplayName("Should not leave temp files after successful write")
        void shouldNotLeaveTempFilesAfterSuccessfulWrite() throws IOException {
            boolean result = configService.writeConfigFile(
                    DnsBlockerType.PIHOLE, "http", "192.168.1.1", 80, "", "token",
                    DnsBlockerType.PIHOLE, "http", "", 80, "", "",
                    "Medium", "Square", "Dark",
                    true, true, true,
                    2,
                    5, 15, 60, 5);

            assertTrue(result);

            // Check that no .tmp files remain
            Path parent = configFilePath.getParent();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, "*.tmp")) {
                for (Path tmpFile : stream) {
                    fail("Temp file should not remain after successful write: " + tmpFile);
                }
            }
        }

        @Test
        @DisplayName("Configuration file should be valid JSON after write")
        void configFileShouldBeValidJsonAfterWrite() throws IOException {
            configService.writeConfigFile(
                    DnsBlockerType.ADGUARD_HOME, "https", "10.0.0.1", 3000, "admin", "secret",
                    DnsBlockerType.PIHOLE, "http", "", 80, "", "",
                    "Large", "Horizontal", "Light",
                    true, true, true,
                    5,
                    10, 20, 30, 40);

            // Read the file content and verify it's valid JSON
            String content = Files.readString(configFilePath);
            assertNotNull(content);
            assertFalse(content.isBlank());

            // Verify it can be read back
            ConfigurationService newService = new ConfigurationService();
            newService.readConfiguration();

            DnsBlockerConfig dns1 = newService.getConfigDNS1();
            assertNotNull(dns1);
            assertEquals("10.0.0.1", dns1.getIPAddress());
            assertEquals(3000, dns1.getPort());
            assertEquals("https", dns1.getScheme());
            assertEquals(DnsBlockerType.ADGUARD_HOME, dns1.platform());
        }

        @Test
        @DisplayName("Multiple rapid writes should not corrupt configuration")
        void multipleRapidWritesShouldNotCorrupt() throws IOException {
            // Perform multiple rapid writes
            for (int i = 0; i < 10; i++) {
                boolean result = configService.writeConfigFile(
                        DnsBlockerType.PIHOLE, "http", "192.168.1." + i, 80 + i, "", "token" + i,
                        DnsBlockerType.PIHOLE, "http", "", 80, "", "",
                        "Medium", "Square", "Dark",
                        true, true, true,
                        2,
                        5, 15, 60, 5);
                assertTrue(result, "Write " + i + " should succeed");
            }

            // Read back and verify the last write succeeded
            ConfigurationService newService = new ConfigurationService();
            newService.readConfiguration();

            DnsBlockerConfig dns1 = newService.getConfigDNS1();
            assertNotNull(dns1);
            assertEquals("192.168.1.9", dns1.getIPAddress());
            assertEquals(89, dns1.getPort());
            assertEquals("token9", dns1.password());
        }
    }
}
