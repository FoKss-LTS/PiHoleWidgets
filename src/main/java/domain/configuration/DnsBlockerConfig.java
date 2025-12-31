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
 * Immutable configuration record for DNS blocker server connection.
 * Supports both Pi-hole and AdGuard Home platforms.
 * Uses Java record syntax for automatic generation of constructor, getters,
 * equals, hashCode, and toString.
 *
 * @param platform  the DNS blocker platform type (Pi-hole or AdGuard Home)
 * @param ipAddress the IP address or hostname of the DNS blocker server
 * @param port      the port number for the API
 * @param scheme    the URL scheme (http or https)
 * @param username  the username for Basic Auth (AdGuard Home) or empty for
 *                  Pi-hole
 * @param password  the password used for authentication (Pi-hole app password or
 *                  AdGuard Home password)
 */
public record DnsBlockerConfig(
        DnsBlockerType platform,
        String ipAddress,
        int port,
        String scheme,
        String username,
        String password) {

    // Default configuration values
    public static final DnsBlockerType DEFAULT_PLATFORM = DnsBlockerType.PIHOLE;
    public static final String DEFAULT_SCHEME = "http";
    public static final String DEFAULT_IP = "pi.hole";
    public static final int DEFAULT_PORT = 80;
    public static final String DEFAULT_USERNAME = "";
    public static final String DEFAULT_PASSWORD = "";

    /**
     * Compact constructor with validation.
     */
    public DnsBlockerConfig {
        // Provide defaults for null values
        if (platform == null) {
            platform = DEFAULT_PLATFORM;
        }
        if (scheme == null || scheme.isBlank()) {
            scheme = DEFAULT_SCHEME;
        }
        if (ipAddress == null) {
            ipAddress = "";
        }
        if (username == null) {
            username = DEFAULT_USERNAME;
        }
        if (password == null) {
            password = DEFAULT_PASSWORD;
        }
        // Validate port range
        if (port <= 0 || port > 65535) {
            port = DEFAULT_PORT;
        }
    }

    /**
     * Creates a DnsBlockerConfig with default port and username (Pi-hole style).
     * For backward compatibility with existing Pi-hole configurations.
     */
    public DnsBlockerConfig(String ipAddress, String scheme, String password) {
        this(DEFAULT_PLATFORM, ipAddress, DEFAULT_PORT, scheme, DEFAULT_USERNAME, password);
    }

    /**
     * Creates a DnsBlockerConfig with default port.
     * For backward compatibility with existing Pi-hole configurations.
     */
    public DnsBlockerConfig(String ipAddress, int port, String scheme, String password) {
        this(DEFAULT_PLATFORM, ipAddress, port, scheme, DEFAULT_USERNAME, password);
    }

    /**
     * Creates a DnsBlockerConfig for AdGuard Home with username and password.
     */
    public static DnsBlockerConfig forAdGuardHome(String ipAddress, int port, String scheme,
            String username, String password) {
        return new DnsBlockerConfig(DnsBlockerType.ADGUARD_HOME, ipAddress, port, scheme,
                username, password);
    }

    /**
     * Creates a DnsBlockerConfig for Pi-hole with app password.
     */
    public static DnsBlockerConfig forPiHole(String ipAddress, int port, String scheme, String password) {
        return new DnsBlockerConfig(DnsBlockerType.PIHOLE, ipAddress, port, scheme,
                DEFAULT_USERNAME, password);
    }

    public String getIPAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    public String getUsername() {
        return username;
    }

    public DnsBlockerType getPlatform() {
        return platform;
    }

    /**
     * Checks if this configuration has a valid IP address.
     */
    public boolean hasValidAddress() {
        return ipAddress != null && !ipAddress.isBlank();
    }

    /**
     * Checks if this configuration has a valid password (app password or password).
     */
    public boolean hasValidPassword() {
        return password != null && !password.isBlank();
    }

    /**
     * Checks if this configuration has a valid username (required for AdGuard
     * Home).
     */
    public boolean hasValidUsername() {
        return username != null && !username.isBlank();
    }

    /**
     * Checks if this configuration is fully valid for the selected platform.
     * Pi-hole requires: address and password
     * AdGuard Home requires: address, username, and password
     */
    public boolean isFullyValid() {
        boolean basicValid = hasValidAddress() && hasValidPassword();

        if (platform == DnsBlockerType.ADGUARD_HOME) {
            return basicValid && hasValidUsername();
        }

        return basicValid;
    }

    /**
     * Builds the base URL for the DNS blocker API.
     */
    public String buildBaseUrl() {
        return scheme + "://" + ipAddress + ":" + port;
    }
}
