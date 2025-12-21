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

package domain.configuration;

/**
 * Immutable configuration record for widget display settings.
 *
 * @param size the widget size (Small, Medium, Large, XXL, Full Screen)
 * @param layout the widget layout (Horizontal, Square)
 * @param theme the UI theme (Dark, Light)
 * @param showLive whether to show live data tile
 * @param showStatus whether to show status tile
 * @param showFluid whether to show fluid percentage tile
 * @param updateStatusSec status tile update interval in seconds
 * @param updateFluidSec fluid tile update interval in seconds
 * @param updateActiveSec active tile update interval in seconds
 * @param updateTopXSec Top X tile update interval in seconds
 */
public record WidgetConfig(
        String size,
        String layout,
        String theme,
        boolean showLive,
        boolean showStatus,
        boolean showFluid,
        int updateStatusSec,
        int updateFluidSec,
        int updateActiveSec,
        int updateTopXSec
) {
    
    // Default values aligned with legacy scheduler behaviour
    public static final String DEFAULT_SIZE = "Medium";
    public static final String DEFAULT_LAYOUT = "Square";
    public static final String DEFAULT_THEME = "Dark";
    public static final int DEFAULT_STATUS_UPDATE_SEC = 5;
    public static final int DEFAULT_FLUID_UPDATE_SEC = 15;
    public static final int DEFAULT_ACTIVE_UPDATE_SEC = 60;
    public static final int DEFAULT_TOPX_UPDATE_SEC = 5;
    
    /**
     * Compact constructor with validation and defaults.
     */
    public WidgetConfig {
        if (size == null || size.isBlank()) {
            size = DEFAULT_SIZE;
        }
        if (layout == null || layout.isBlank()) {
            layout = DEFAULT_LAYOUT;
        }
        if (theme == null || theme.isBlank()) {
            theme = DEFAULT_THEME;
        }
        if (updateStatusSec <= 0) {
            updateStatusSec = DEFAULT_STATUS_UPDATE_SEC;
        }
        if (updateFluidSec <= 0) {
            updateFluidSec = DEFAULT_FLUID_UPDATE_SEC;
        }
        if (updateActiveSec <= 0) {
            updateActiveSec = DEFAULT_ACTIVE_UPDATE_SEC;
        }
        if (updateTopXSec <= 0) {
            updateTopXSec = DEFAULT_TOPX_UPDATE_SEC;
        }
    }
    
    /**
     * Creates a WidgetConfig with default values for display options and intervals.
     */
    public WidgetConfig(String size, String layout) {
        this(size, layout, DEFAULT_THEME, true, true, true,
             DEFAULT_STATUS_UPDATE_SEC, DEFAULT_FLUID_UPDATE_SEC, DEFAULT_ACTIVE_UPDATE_SEC, DEFAULT_TOPX_UPDATE_SEC);
    }
    
    /**
     * Creates a WidgetConfig with size, layout, and theme.
     */
    public WidgetConfig(String size, String layout, String theme) {
        this(size, layout, theme, true, true, true,
             DEFAULT_STATUS_UPDATE_SEC, DEFAULT_FLUID_UPDATE_SEC, DEFAULT_ACTIVE_UPDATE_SEC, DEFAULT_TOPX_UPDATE_SEC);
    }
    
    /**
     * Creates a default WidgetConfig.
     */
    public static WidgetConfig defaultConfig() {
        return new WidgetConfig(DEFAULT_SIZE, DEFAULT_LAYOUT, DEFAULT_THEME);
    }
    
    // Legacy getter methods for backward compatibility
    
    public String getSize() {
        return size;
    }
    
    public String getLayout() {
        return layout;
    }
    
    public String getTheme() {
        return theme;
    }
    
    public boolean isShow_live() {
        return showLive;
    }
    
    public boolean isShow_status() {
        return showStatus;
    }
    
    public boolean isShow_fluid() {
        return showFluid;
    }
    
    public int getUpdate_status_sec() {
        return updateStatusSec;
    }
    
    public int getUpdate_fluid_sec() {
        return updateFluidSec;
    }
    
    public int getUpdate_active_sec() {
        return updateActiveSec;
    }
    
    public int getUpdate_topx_sec() {
        return updateTopXSec;
    }
}
