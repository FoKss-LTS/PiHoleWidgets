package services.configuration;

import domain.configuration.DnsBlockerConfig;
import domain.configuration.DnsBlockerType;
import domain.configuration.WidgetConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
                    "Authentication Token": "token123"
                  },
                  "Widget": {
                    "Size": "Large",
                    "Layout": "Horizontal",
                    "Theme": "Light",
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
        assertEquals("token123", dns1.getAUTH());

        WidgetConfig widget = configService.getWidgetConfig();
        assertNotNull(widget);
        assertEquals("Large", widget.getSize());
        assertEquals("Horizontal", widget.getLayout());
        assertEquals("Light", widget.getTheme());
        assertEquals(10, widget.getUpdate_status_sec());
        assertEquals(20, widget.getUpdate_fluid_sec());
        assertEquals(30, widget.getUpdate_active_sec());
        assertEquals(40, widget.getUpdate_topx_sec());
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
                    "Authentication Token": "token"
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
                10, 20, 30, 40);

        // Create new service instance and read
        ConfigurationService newService = new ConfigurationService();
        newService.readConfiguration();

        DnsBlockerConfig dns1 = newService.getConfigDNS1();
        assertNotNull(dns1);
        assertEquals("192.168.1.1", dns1.getIPAddress());
        assertEquals(443, dns1.getPort());
        assertEquals("https", dns1.getScheme());
        assertEquals("mytoken", dns1.getAUTH());

        WidgetConfig widget = newService.getWidgetConfig();
        assertNotNull(widget);
        assertEquals("Large", widget.getSize());
        assertEquals("Horizontal", widget.getLayout());
        assertEquals("Light", widget.getTheme());
        assertEquals(10, widget.getUpdate_status_sec());
        assertEquals(20, widget.getUpdate_fluid_sec());
        assertEquals(30, widget.getUpdate_active_sec());
        assertEquals(40, widget.getUpdate_topx_sec());
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
                5, 15, 60, 5);

        assertTrue(result);

        // Read back and verify default theme is used
        ConfigurationService newService = new ConfigurationService();
        newService.readConfiguration();
        assertEquals(WidgetConfig.DEFAULT_THEME, newService.getWidgetConfig().getTheme());
    }
}
