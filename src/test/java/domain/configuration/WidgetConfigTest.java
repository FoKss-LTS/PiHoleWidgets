package domain.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WidgetConfig record.
 */
class WidgetConfigTest {

    @Test
    void testDefaultConstructor() {
        var config = new WidgetConfig(
            "Large", "Horizontal", "Light",
            true, false, true,
            10, 20, 30, 40
        );
        
        assertEquals("Large", config.size());
        assertEquals("Horizontal", config.layout());
        assertEquals("Light", config.theme());
        assertTrue(config.showLive());
        assertFalse(config.showStatus());
        assertTrue(config.showFluid());
        assertEquals(10, config.updateStatusSec());
        assertEquals(20, config.updateFluidSec());
        assertEquals(30, config.updateActiveSec());
        assertEquals(40, config.updateTopXSec());
    }

    @Test
    void testConstructorWithSizeAndLayout() {
        var config = new WidgetConfig("Small", "Square");
        
        assertEquals("Small", config.size());
        assertEquals("Square", config.layout());
        assertEquals(WidgetConfig.DEFAULT_THEME, config.theme());
        assertTrue(config.showLive());
        assertTrue(config.showStatus());
        assertTrue(config.showFluid());
        assertEquals(WidgetConfig.DEFAULT_STATUS_UPDATE_SEC, config.updateStatusSec());
        assertEquals(WidgetConfig.DEFAULT_FLUID_UPDATE_SEC, config.updateFluidSec());
        assertEquals(WidgetConfig.DEFAULT_ACTIVE_UPDATE_SEC, config.updateActiveSec());
        assertEquals(WidgetConfig.DEFAULT_TOPX_UPDATE_SEC, config.updateTopXSec());
    }

    @Test
    void testConstructorWithSizeLayoutAndTheme() {
        var config = new WidgetConfig("Medium", "Horizontal", "Dark");
        
        assertEquals("Medium", config.size());
        assertEquals("Horizontal", config.layout());
        assertEquals("Dark", config.theme());
        assertTrue(config.showLive());
        assertTrue(config.showStatus());
        assertTrue(config.showFluid());
    }

    @Test
    void testDefaultConfig() {
        var config = WidgetConfig.defaultConfig();
        
        assertEquals(WidgetConfig.DEFAULT_SIZE, config.size());
        assertEquals(WidgetConfig.DEFAULT_LAYOUT, config.layout());
        assertEquals(WidgetConfig.DEFAULT_THEME, config.theme());
        assertTrue(config.showLive());
        assertTrue(config.showStatus());
        assertTrue(config.showFluid());
    }

    @Test
    void testNullSizeDefaultsToMedium() {
        var config = new WidgetConfig(null, "Square", "Dark", true, true, true, 5, 15, 60, 5);
        assertEquals(WidgetConfig.DEFAULT_SIZE, config.size());
    }

    @Test
    void testBlankSizeDefaultsToMedium() {
        var config = new WidgetConfig("   ", "Square", "Dark", true, true, true, 5, 15, 60, 5);
        assertEquals(WidgetConfig.DEFAULT_SIZE, config.size());
    }

    @Test
    void testNullLayoutDefaultsToSquare() {
        var config = new WidgetConfig("Medium", null, "Dark", true, true, true, 5, 15, 60, 5);
        assertEquals(WidgetConfig.DEFAULT_LAYOUT, config.layout());
    }

    @Test
    void testBlankLayoutDefaultsToSquare() {
        var config = new WidgetConfig("Medium", "   ", "Dark", true, true, true, 5, 15, 60, 5);
        assertEquals(WidgetConfig.DEFAULT_LAYOUT, config.layout());
    }

    @Test
    void testNullThemeDefaultsToDark() {
        var config = new WidgetConfig("Medium", "Square", null, true, true, true, 5, 15, 60, 5);
        assertEquals(WidgetConfig.DEFAULT_THEME, config.theme());
    }

    @Test
    void testBlankThemeDefaultsToDark() {
        var config = new WidgetConfig("Medium", "Square", "   ", true, true, true, 5, 15, 60, 5);
        assertEquals(WidgetConfig.DEFAULT_THEME, config.theme());
    }

    @Test
    void testInvalidUpdateIntervalsDefaultToDefaults() {
        var config1 = new WidgetConfig("Medium", "Square", "Dark", true, true, true, 0, 15, 60, 5);
        assertEquals(WidgetConfig.DEFAULT_STATUS_UPDATE_SEC, config1.updateStatusSec());
        
        var config2 = new WidgetConfig("Medium", "Square", "Dark", true, true, true, 5, -1, 60, 5);
        assertEquals(WidgetConfig.DEFAULT_FLUID_UPDATE_SEC, config2.updateFluidSec());
        
        var config3 = new WidgetConfig("Medium", "Square", "Dark", true, true, true, 5, 15, 0, 5);
        assertEquals(WidgetConfig.DEFAULT_ACTIVE_UPDATE_SEC, config3.updateActiveSec());
        
        var config4 = new WidgetConfig("Medium", "Square", "Dark", true, true, true, 5, 15, 60, -5);
        assertEquals(WidgetConfig.DEFAULT_TOPX_UPDATE_SEC, config4.updateTopXSec());
    }

    @Test
    void testValidUpdateIntervals() {
        var config = new WidgetConfig("Medium", "Square", "Dark", true, true, true, 1, 2, 3, 4);
        
        assertEquals(1, config.updateStatusSec());
        assertEquals(2, config.updateFluidSec());
        assertEquals(3, config.updateActiveSec());
        assertEquals(4, config.updateTopXSec());
    }

    @Test
    void testLegacyGetters() {
        var config = new WidgetConfig("Large", "Horizontal", "Light", true, false, true, 10, 20, 30, 40);
        
        assertEquals("Large", config.getSize());
        assertEquals("Horizontal", config.getLayout());
        assertEquals("Light", config.getTheme());
        assertTrue(config.isShow_live());
        assertFalse(config.isShow_status());
        assertTrue(config.isShow_fluid());
        assertEquals(10, config.getUpdate_status_sec());
        assertEquals(20, config.getUpdate_fluid_sec());
        assertEquals(30, config.getUpdate_active_sec());
        assertEquals(40, config.getUpdate_topx_sec());
    }

    @Test
    void testRecordEquality() {
        var config1 = new WidgetConfig("Medium", "Square", "Dark", true, true, true, 5, 15, 60, 5);
        var config2 = new WidgetConfig("Medium", "Square", "Dark", true, true, true, 5, 15, 60, 5);
        var config3 = new WidgetConfig("Large", "Square", "Dark", true, true, true, 5, 15, 60, 5);
        
        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void testToString() {
        var config = new WidgetConfig("Medium", "Square", "Dark", true, true, true, 5, 15, 60, 5);
        var toString = config.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("Medium"));
        assertTrue(toString.contains("Square"));
    }
}

