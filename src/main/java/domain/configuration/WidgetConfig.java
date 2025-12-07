/*
 *
 *  Copyright (C) 2022.  Reda ELFARISSI aka foxy999
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
 * @param showLive whether to show live data tile
 * @param showStatus whether to show status tile
 * @param showFluid whether to show fluid percentage tile
 * @param updateStatusSec status tile update interval in seconds
 * @param updateFluidSec fluid tile update interval in seconds
 * @param updateActiveSec active tile update interval in seconds
 */
public record WidgetConfig(
        String size,
        String layout,
        boolean showLive,
        boolean showStatus,
        boolean showFluid,
        int updateStatusSec,
        int updateFluidSec,
        int updateActiveSec
) {
    
    // Default values
    public static final String DEFAULT_SIZE = "Medium";
    public static final String DEFAULT_LAYOUT = "Square";
    public static final int DEFAULT_UPDATE_INTERVAL = 5;
    
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
        if (updateStatusSec <= 0) {
            updateStatusSec = DEFAULT_UPDATE_INTERVAL;
        }
        if (updateFluidSec <= 0) {
            updateFluidSec = DEFAULT_UPDATE_INTERVAL;
        }
        if (updateActiveSec <= 0) {
            updateActiveSec = DEFAULT_UPDATE_INTERVAL;
        }
    }
    
    /**
     * Creates a WidgetConfig with default values for display options and intervals.
     */
    public WidgetConfig(String size, String layout) {
        this(size, layout, true, true, true, 
             DEFAULT_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL);
    }
    
    /**
     * Creates a default WidgetConfig.
     */
    public static WidgetConfig defaultConfig() {
        return new WidgetConfig(DEFAULT_SIZE, DEFAULT_LAYOUT);
    }
    
    // Legacy getter methods for backward compatibility
    
    public String getSize() {
        return size;
    }
    
    public String getLayout() {
        return layout;
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
}
