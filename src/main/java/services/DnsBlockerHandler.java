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

package services;

/**
 * Common interface for DNS blocker API handlers.
 * Implementations exist for Pi-hole and AdGuard Home platforms.
 * 
 * All methods return String data (typically JSON) or empty string on failure,
 * allowing the widget controller to handle display uniformly.
 */
public interface DnsBlockerHandler {

    /**
     * Authenticates with the DNS blocker server.
     * For Pi-hole: establishes a session and retrieves session ID.
     * For AdGuard Home: validates Basic Auth credentials.
     *
     * @return true if authentication succeeded, false otherwise
     */
    boolean authenticate();

    /**
     * Retrieves DNS blocker statistics as raw JSON string.
     * Includes data like total queries, blocked queries, percentage blocked, etc.
     *
     * @return JSON string with statistics, or empty string on failure
     */
    String getStats();

    /**
     * Retrieves the last blocked domain.
     *
     * @return the domain name that was most recently blocked, or empty string on
     *         failure
     */
    String getLastBlocked();

    /**
     * Retrieves the DNS blocker version information.
     *
     * @return version string, or empty string on failure
     */
    String getVersion();

    /**
     * Retrieves top X blocked domains as raw JSON string.
     *
     * @param count the number of top blocked domains to retrieve
     * @return JSON string with top blocked domains, or empty string on failure
     */
    String getTopXBlocked(int count);

    /**
     * Retrieves the gravity/filter list last update time as a formatted string.
     * For Pi-hole: gravity database update time.
     * For AdGuard Home: filter list update time.
     *
     * @return formatted time string, or empty string on failure
     */
    String getGravityLastUpdate();

    /**
     * Changes the current DNS blocking status.
     * For Pi-hole: POST to /dns/blocking.
     * For AdGuard Home: POST to /control/dns_config with protection_enabled.
     *
     * @param blocking     whether to enable (true) or disable (false) DNS blocking
     * @param timerSeconds optional timer in seconds for automatic reversal (null
     *                     for no timer)
     * @return raw JSON response from the server, or empty string on failure
     */
    String setDnsBlocking(boolean blocking, Integer timerSeconds);

    /**
     * Retrieves the current DNS blocking status.
     * For Pi-hole: GET /dns/blocking.
     * For AdGuard Home: GET /control/status.
     *
     * @return raw JSON response with blocking status, or empty string on failure
     */
    String getDnsBlockingStatus();
}
