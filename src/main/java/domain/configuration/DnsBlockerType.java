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
 * Represents the type of DNS blocker platform.
 * Used to distinguish between Pi-hole and AdGuard Home implementations.
 */
public enum DnsBlockerType {
    /**
     * Pi-hole DNS blocker platform.
     * Uses session-based authentication with app passwords.
     */
    PIHOLE("Pi-hole"),

    /**
     * AdGuard Home DNS blocker platform.
     * Uses HTTP Basic Authentication with username/password.
     */
    ADGUARD_HOME("AdGuard Home");

    private final String displayName;

    /**
     * Constructor for DnsBlockerType enum.
     *
     * @param displayName the human-readable name for this platform
     */
    DnsBlockerType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the display name for this DNS blocker type.
     *
     * @return the human-readable platform name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a string into a DnsBlockerType.
     * Case-insensitive matching by name or display name.
     *
     * @param value the string value to parse
     * @return the matching DnsBlockerType, or PIHOLE if not found (for backward
     *         compatibility)
     */
    public static DnsBlockerType fromString(String value) {
        if (value == null || value.isBlank()) {
            return PIHOLE; // Default for backward compatibility
        }

        String normalized = value.trim().toUpperCase();

        // Try to match by enum name first
        try {
            return DnsBlockerType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // If that fails, match by display name
            for (DnsBlockerType type : values()) {
                if (type.displayName.equalsIgnoreCase(value)) {
                    return type;
                }
            }
        }

        // Default to PIHOLE for backward compatibility
        return PIHOLE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
