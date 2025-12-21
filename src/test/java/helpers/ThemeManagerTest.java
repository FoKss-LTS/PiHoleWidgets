package helpers;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThemeManager utility class.
 */
class ThemeManagerTest {

    @Test
    void testGetThemeCssPath() {
        assertEquals("/controllers/dark-theme.css", ThemeManager.getThemeCssPath("Dark"));
        assertEquals("/controllers/dark-theme.css", ThemeManager.getThemeCssPath("dark"));
        assertEquals("/controllers/dark-theme.css", ThemeManager.getThemeCssPath("DARK"));
        assertEquals("/controllers/light-theme.css", ThemeManager.getThemeCssPath("Light"));
        assertEquals("/controllers/light-theme.css", ThemeManager.getThemeCssPath("light"));
        assertEquals("/controllers/light-theme.css", ThemeManager.getThemeCssPath("LIGHT"));
    }

    @Test
    void testGetThemeCssPathWithInvalidTheme() {
        // Should default to dark theme
        assertEquals("/controllers/dark-theme.css", ThemeManager.getThemeCssPath("Invalid"));
        assertEquals("/controllers/dark-theme.css", ThemeManager.getThemeCssPath(null));
    }

    @Test
    void testGetBackgroundColor() {
        assertEquals(ThemeManager.DARK_BACKGROUND, ThemeManager.getBackgroundColor("Dark"));
        assertEquals(ThemeManager.LIGHT_BACKGROUND, ThemeManager.getBackgroundColor("Light"));
        assertEquals(ThemeManager.DARK_BACKGROUND, ThemeManager.getBackgroundColor("dark"));
        assertEquals(ThemeManager.LIGHT_BACKGROUND, ThemeManager.getBackgroundColor("light"));
    }

    @Test
    void testGetBackgroundColorWithInvalidTheme() {
        // Should default to dark
        assertEquals(ThemeManager.DARK_BACKGROUND, ThemeManager.getBackgroundColor("Invalid"));
        assertEquals(ThemeManager.DARK_BACKGROUND, ThemeManager.getBackgroundColor(null));
    }

    @Test
    void testGetTileBackgroundColor() {
        assertEquals(ThemeManager.DARK_TILE_BACKGROUND, ThemeManager.getTileBackgroundColor("Dark"));
        assertEquals(ThemeManager.LIGHT_TILE_BACKGROUND, ThemeManager.getTileBackgroundColor("Light"));
    }

    @Test
    void testGetForegroundColor() {
        assertEquals(ThemeManager.DARK_FOREGROUND, ThemeManager.getForegroundColor("Dark"));
        assertEquals(ThemeManager.LIGHT_FOREGROUND, ThemeManager.getForegroundColor("Light"));
    }

    @Test
    void testGetTitleColor() {
        assertEquals(ThemeManager.DARK_TITLE_TEXT, ThemeManager.getTitleColor("Dark"));
        assertEquals(ThemeManager.LIGHT_TITLE_TEXT, ThemeManager.getTitleColor("Light"));
    }

    @Test
    void testGetValueColor() {
        assertEquals(ThemeManager.DARK_VALUE_TEXT, ThemeManager.getValueColor("Dark"));
        assertEquals(ThemeManager.LIGHT_VALUE_TEXT, ThemeManager.getValueColor("Light"));
    }

    @Test
    void testGetTextColor() {
        assertEquals(ThemeManager.DARK_TEXT, ThemeManager.getTextColor("Dark"));
        assertEquals(ThemeManager.LIGHT_TEXT, ThemeManager.getTextColor("Light"));
    }

    @Test
    void testGetMutedTextColor() {
        assertEquals(ThemeManager.DARK_MUTED_TEXT, ThemeManager.getMutedTextColor("Dark"));
        assertEquals(ThemeManager.LIGHT_MUTED_TEXT, ThemeManager.getMutedTextColor("Light"));
    }

    @Test
    void testGetBackgroundColorHex() {
        String darkHex = ThemeManager.getBackgroundColorHex("Dark");
        assertNotNull(darkHex);
        assertTrue(darkHex.startsWith("#"));
        assertEquals(7, darkHex.length()); // #RRGGBB format
        
        String lightHex = ThemeManager.getBackgroundColorHex("Light");
        assertNotNull(lightHex);
        assertTrue(lightHex.startsWith("#"));
        assertEquals(7, lightHex.length());
    }

    @Test
    void testGetTileBackgroundColorHex() {
        String darkHex = ThemeManager.getTileBackgroundColorHex("Dark");
        assertNotNull(darkHex);
        assertTrue(darkHex.startsWith("#"));
        assertEquals(7, darkHex.length());
        
        String lightHex = ThemeManager.getTileBackgroundColorHex("Light");
        assertNotNull(lightHex);
        assertTrue(lightHex.startsWith("#"));
        assertEquals(7, lightHex.length());
    }

    @Test
    void testIsLightTheme() {
        assertFalse(ThemeManager.isLightTheme("Dark"));
        assertTrue(ThemeManager.isLightTheme("Light"));
        assertFalse(ThemeManager.isLightTheme("dark"));
        assertTrue(ThemeManager.isLightTheme("light"));
        assertFalse(ThemeManager.isLightTheme("DARK"));
        assertTrue(ThemeManager.isLightTheme("LIGHT"));
        assertFalse(ThemeManager.isLightTheme("Invalid"));
        assertFalse(ThemeManager.isLightTheme(null));
    }

    @Test
    void testApplyThemeWithNullScene() {
        // Should not throw exception
        assertDoesNotThrow(() -> ThemeManager.applyTheme(null, "Dark"));
    }

    @Test
    void testApplyThemeWithValidScene() {
        Scene scene = new Scene(new VBox());
        
        // Should not throw exception
        assertDoesNotThrow(() -> ThemeManager.applyTheme(scene, "Dark"));
        assertDoesNotThrow(() -> ThemeManager.applyTheme(scene, "Light"));
    }

    @Test
    void testConstants() {
        assertNotNull(ThemeManager.DARK_THEME);
        assertNotNull(ThemeManager.LIGHT_THEME);
        assertNotNull(ThemeManager.DEFAULT_THEME);
        assertEquals(ThemeManager.DARK_THEME, ThemeManager.DEFAULT_THEME);
        
        assertNotNull(ThemeManager.DARK_BACKGROUND);
        assertNotNull(ThemeManager.DARK_TILE_BACKGROUND);
        assertNotNull(ThemeManager.DARK_FOREGROUND);
        assertNotNull(ThemeManager.DARK_TEXT);
        assertNotNull(ThemeManager.DARK_TITLE_TEXT);
        assertNotNull(ThemeManager.DARK_MUTED_TEXT);
        assertNotNull(ThemeManager.DARK_VALUE_TEXT);
        
        assertNotNull(ThemeManager.LIGHT_BACKGROUND);
        assertNotNull(ThemeManager.LIGHT_TILE_BACKGROUND);
        assertNotNull(ThemeManager.LIGHT_FOREGROUND);
        assertNotNull(ThemeManager.LIGHT_TEXT);
        assertNotNull(ThemeManager.LIGHT_TITLE_TEXT);
        assertNotNull(ThemeManager.LIGHT_MUTED_TEXT);
        assertNotNull(ThemeManager.LIGHT_VALUE_TEXT);
    }
}

