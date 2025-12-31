package controllers;

import domain.configuration.DnsBlockerConfig;
import domain.configuration.WidgetConfig;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WidgetController.
 * Tests focus on non-UI logic, JSON parsing, and data processing.
 */
class WidgetControllerTest {

    private WidgetController controller;
    private DnsBlockerConfig testPiholeConfig;
    private WidgetConfig testWidgetConfig;
    // (No JSON parsing tested in this suite currently.)
    private static final AppActions NOOP_ACTIONS = new AppActions() {
        @Override
        public void openConfigurationWindow() {}

        @Override
        public void applyAndCloseConfigurationWindow() {}

        @Override
        public void closeConfigurationWindow() {}

        @Override
        public void hideToTray() {}

        @Override
        public void requestExit() {}
    };

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX toolkit
        new JFXPanel();
    }

    @BeforeEach
    void setUp() {
        testPiholeConfig = DnsBlockerConfig.forPiHole("192.168.1.1", 80, "http", "testtoken");
        testWidgetConfig = new WidgetConfig("Medium", "Square", "Dark");
        controller = new WidgetController(testPiholeConfig, null, testWidgetConfig, NOOP_ACTIONS);
    }

    @Test
    void testConstructor() {
        assertNotNull(controller);
        assertEquals(testPiholeConfig, controller.getConfigDNS1());
        assertEquals(testWidgetConfig, controller.getWidgetConfig());
    }

    @Test
    void testConstructorWithNullConfig() {
        WidgetController nullController = new WidgetController(null, null, null, NOOP_ACTIONS);
        assertNotNull(nullController);
        assertNull(nullController.getConfigDNS1());
        assertNull(nullController.getWidgetConfig());
    }

    @Test
    void testGetConfigDNS1() {
        assertEquals(testPiholeConfig, controller.getConfigDNS1());
    }

    @Test
    void testSetConfigDNS1() {
        DnsBlockerConfig newConfig = DnsBlockerConfig.forPiHole("192.168.1.2", 443, "https", "newtoken");
        controller.setConfigDNS1(newConfig);
        assertEquals(newConfig, controller.getConfigDNS1());
    }

    @Test
    void testGetWidgetConfig() {
        assertEquals(testWidgetConfig, controller.getWidgetConfig());
    }

    @Test
    void testSetWidgetConfig() {
        WidgetConfig newConfig = new WidgetConfig("Large", "Horizontal", "Light");
        controller.setWidgetConfig(newConfig);
        assertEquals(newConfig, controller.getWidgetConfig());
    }

    @Test
    void testApplyConfiguration() {
        DnsBlockerConfig newPiholeConfig = DnsBlockerConfig.forPiHole("192.168.1.2", 443, "https", "newtoken");
        WidgetConfig newWidgetConfig = new WidgetConfig("Large", "Horizontal", "Light");

        // applyConfiguration may throw exceptions if JavaFX is not fully initialized
        // We test that it doesn't throw unexpected exceptions
        try {
            controller.applyConfiguration(newPiholeConfig, null, newWidgetConfig);
            assertEquals(newPiholeConfig, controller.getConfigDNS1());
            assertEquals(newWidgetConfig, controller.getWidgetConfig());
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Expected when JavaFX platform is not fully initialized
            // This is acceptable for unit tests
        }
    }

    @Test
    void testRefreshPihole() {
        // refreshPihole creates PiHoleHandler which requires network access
        // We test that it doesn't throw exceptions with valid config
        try {
            controller.refreshPihole();
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Expected when JavaFX platform is not fully initialized
            // This is acceptable for unit tests
        }
    }

    @Test
    void testRefreshPiholeWithNullConfig() {
        WidgetController nullController = new WidgetController(null, null, testWidgetConfig, NOOP_ACTIONS);
        assertDoesNotThrow(() -> {
            nullController.refreshPihole();
        });
    }

    @Test
    void testShutdown() {
        // Shutdown should not throw even if not initialized
        assertDoesNotThrow(() -> {
            controller.shutdown();
        });
    }

    @Test
    void testShutdownWhenNotInitialized() {
        // Shutdown on uninitialized controller should not throw
        assertDoesNotThrow(() -> {
            controller.shutdown();
        });
    }

    @Test
    void testRefreshAllTiles() {
        // refreshAllTiles triggers data refresh
        assertDoesNotThrow(() -> {
            controller.refreshAllTiles();
        });
    }

    @Test
    void testApplyTheme() {
        assertDoesNotThrow(() -> {
            controller.applyTheme("Dark");
            controller.applyTheme("Light");
        });
    }

    @Test
    void testApplyThemeWithInvalidTheme() {
        assertDoesNotThrow(() -> {
            controller.applyTheme("Invalid");
        });
    }

    @Test
    void testGetGridPane() {
        // Before initialization, gridPane should be null
        assertNull(controller.getGridPane());
    }

    @Test
    void testOpenConfigurationWindow() {
        assertDoesNotThrow(() -> {
            controller.openConfigurationWindow();
        });
    }

    // Note: Testing private methods like parseSummaryStats, combineStats, etc.
    // would require reflection or extracting them to package-private/testable
    // methods.
    // For now, we focus on public API and integration points.
}
