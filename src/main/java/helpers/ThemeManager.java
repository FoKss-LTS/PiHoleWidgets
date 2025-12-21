/*
 *
 *  Copyright (C) 2022 - 2025.  Reda ELFARISSI aka FoKss-LTS
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package helpers;

import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for managing application themes.
 */
public final class ThemeManager {

    private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));

    public static final String DARK_THEME = "Dark";
    public static final String LIGHT_THEME = "Light";
    public static final String DEFAULT_THEME = DARK_THEME;

    private static final String DARK_THEME_CSS = "/controllers/dark-theme.css";
    private static final String LIGHT_THEME_CSS = "/controllers/light-theme.css";

    // Dark theme colors
    public static final Color DARK_BACKGROUND = Color.web("#101214");
    public static final Color DARK_TILE_BACKGROUND = Color.web("#1a1d21");
    public static final Color DARK_FOREGROUND = Color.web("#e0e0e0");  // Light color for text visibility
    public static final Color DARK_TEXT = Color.web("#e0e0e0");
    public static final Color DARK_TITLE_TEXT = Color.web("#ffffff");
    public static final Color DARK_MUTED_TEXT = Color.web("#888888");
    public static final Color DARK_VALUE_TEXT = Color.web("#4a9eff");

    // Light theme colors
    public static final Color LIGHT_BACKGROUND = Color.web("#e9ecef");
    public static final Color LIGHT_TILE_BACKGROUND = Color.web("#ffffff");
    public static final Color LIGHT_FOREGROUND = Color.web("#212529");  // Dark color for text visibility
    public static final Color LIGHT_TEXT = Color.web("#212529");
    public static final Color LIGHT_TITLE_TEXT = Color.web("#1a1a2e");
    public static final Color LIGHT_MUTED_TEXT = Color.web("#6c757d");
    public static final Color LIGHT_VALUE_TEXT = Color.web("#0d6efd");

    private ThemeManager() {
        // Utility class - no instantiation
    }

    private static void log(String message) {
        if (VERBOSE) {
            LOGGER.log(Level.FINE, () -> "[ThemeManager] " + message);
        }
    }

    /**
     * Gets the CSS resource path for the given theme.
     *
     * @param theme the theme name (Dark or Light)
     * @return the CSS resource path
     */
    public static String getThemeCssPath(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme) ? LIGHT_THEME_CSS : DARK_THEME_CSS;
    }

    /**
     * Gets the CSS resource URL for the given theme.
     *
     * @param theme the theme name (Dark or Light)
     * @return the CSS resource URL as string
     */
    public static String getThemeCssUrl(String theme) {
        String path = getThemeCssPath(theme);
        try {
            return Objects.requireNonNull(ThemeManager.class.getResource(path)).toExternalForm();
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "Theme CSS not found: " + path);
            return null;
        }
    }

    /**
     * Applies the specified theme to a scene.
     *
     * @param scene the scene to apply the theme to
     * @param theme the theme name (Dark or Light)
     */
    public static void applyTheme(Scene scene, String theme) {
        if (scene == null) {
            log("Cannot apply theme to null scene");
            return;
        }

        log("Applying theme: " + theme);

        // Remove existing theme stylesheets
        scene.getStylesheets().removeIf(s -> s.contains("dark-theme.css") || s.contains("light-theme.css"));

        // Add the new theme stylesheet
        String cssUrl = getThemeCssUrl(theme);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl);
            log("Theme CSS applied: " + cssUrl);
        }
    }

    /**
     * Gets the background color for the given theme.
     *
     * @param theme the theme name
     * @return the background color
     */
    public static Color getBackgroundColor(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme) ? LIGHT_BACKGROUND : DARK_BACKGROUND;
    }

    /**
     * Gets the tile background color for the given theme.
     *
     * @param theme the theme name
     * @return the tile background color
     */
    public static Color getTileBackgroundColor(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme) ? LIGHT_TILE_BACKGROUND : DARK_TILE_BACKGROUND;
    }

    /**
     * Gets the foreground color for the given theme.
     *
     * @param theme the theme name
     * @return the foreground color
     */
    public static Color getForegroundColor(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme) ? LIGHT_FOREGROUND : DARK_FOREGROUND;
    }

    /**
     * Gets the title text color for the given theme.
     *
     * @param theme the theme name
     * @return the title text color
     */
    public static Color getTitleColor(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme) ? LIGHT_TITLE_TEXT : DARK_TITLE_TEXT;
    }

    /**
     * Gets the value text color for the given theme.
     *
     * @param theme the theme name
     * @return the value text color
     */
    public static Color getValueColor(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme) ? LIGHT_VALUE_TEXT : DARK_VALUE_TEXT;
    }

    /**
     * Gets the text color for the given theme.
     *
     * @param theme the theme name
     * @return the text color
     */
    public static Color getTextColor(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme) ? LIGHT_TEXT : DARK_TEXT;
    }

    /**
     * Gets the muted text color for the given theme.
     *
     * @param theme the theme name
     * @return the muted text color
     */
    public static Color getMutedTextColor(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme) ? LIGHT_MUTED_TEXT : DARK_MUTED_TEXT;
    }

    /**
     * Gets the background color as a CSS-compatible hex string.
     *
     * @param theme the theme name
     * @return the hex color string (e.g., "#101214")
     */
    public static String getBackgroundColorHex(String theme) {
        return colorToHex(getBackgroundColor(theme));
    }

    /**
     * Gets the tile background color as a CSS-compatible hex string.
     *
     * @param theme the theme name
     * @return the hex color string
     */
    public static String getTileBackgroundColorHex(String theme) {
        return colorToHex(getTileBackgroundColor(theme));
    }

    /**
     * Checks if the given theme is the light theme.
     *
     * @param theme the theme name
     * @return true if light theme
     */
    public static boolean isLightTheme(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme);
    }

    /**
     * Converts a JavaFX Color to a hex string.
     *
     * @param color the color to convert
     * @return the hex string (e.g., "#ff0000")
     */
    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}

