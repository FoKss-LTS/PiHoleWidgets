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
 * Immutable configuration record for Pi-hole DNS server connection.
 * Uses Java record syntax for automatic generation of constructor, getters,
 * equals, hashCode, and toString.
 *
 * @param ipAddress the IP address or hostname of the Pi-hole server
 * @param port      the port number for the Pi-hole API
 * @param scheme    the URL scheme (http or https)
 * @param authToken the authentication token for the Pi-hole API
 */
public record PiholeConfig(
        String ipAddress,
        int port,
        String scheme,
        String authToken) {

    // Default configuration values
    public static final String DEFAULT_SCHEME = "http";
    public static final String DEFAULT_IP = "pi.hole";
    public static final int DEFAULT_PORT = 80;
    public static final String DEFAULT_AUTH_TOKEN = "";

    /**
     * Compact constructor with validation.
     */
    public PiholeConfig {
        // Provide defaults for null values
        if (scheme == null || scheme.isBlank()) {
            scheme = DEFAULT_SCHEME;
        }
        if (ipAddress == null) {
            ipAddress = "";
        }
        if (authToken == null) {
            authToken = DEFAULT_AUTH_TOKEN;
        }
        // Validate port range
        if (port <= 0 || port > 65535) {
            port = DEFAULT_PORT;
        }
    }

    /**
     * Creates a PiholeConfig with default port.
     */
    public PiholeConfig(String ipAddress, String scheme, String authToken) {
        this(ipAddress, DEFAULT_PORT, scheme, authToken);
    }

    // Legacy getter methods for backward compatibility
    // These delegate to the record's accessor methods

    public String getIPAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    public String getAUTH() {
        return authToken;
    }

    /**
     * Checks if this configuration has a valid IP address.
     */
    public boolean hasValidAddress() {
        return ipAddress != null && !ipAddress.isBlank();
    }

    /**
     * Checks if this configuration has a valid auth token (app password).
     */
    public boolean hasValidAuthToken() {
        return authToken != null && !authToken.isBlank();
    }

    /**
     * Checks if this configuration is fully valid (both address and auth token).
     */
    public boolean isFullyValid() {
        return hasValidAddress() && hasValidAuthToken();
    }

    /**
     * Builds the base URL for the Pi-hole API.
     */
    public String buildBaseUrl() {
        return scheme + "://" + ipAddress + ":" + port;
    }
}
