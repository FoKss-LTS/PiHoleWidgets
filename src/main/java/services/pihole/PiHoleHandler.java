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

package services.pihole;

import com.fasterxml.jackson.databind.JsonNode;
import domain.configuration.DnsBlockerConfig;
import helpers.HttpClientUtil;
import helpers.HttpClientUtil.HttpResponsePayload;
import services.DnsBlockerHandler;
import services.configuration.ConfigurationService;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for Pi-hole API communication.
 * Manages authentication and data retrieval from Pi-hole servers.
 * Implements the DnsBlockerHandler interface for platform abstraction.
 */
public class PiHoleHandler implements DnsBlockerHandler {

    // ==================== Constants ====================

    private static final Logger LOGGER = Logger.getLogger(PiHoleHandler.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("dnsbloquer.verbose", "false"));

    private static final String API_PATH = "/api";
    private static final String AUTH_ENDPOINT = "/auth";
    private static final String VERSION_ENDPOINT = "/info/version";
    private static final String STATS_SUMMARY_ENDPOINT = "/stats/summary";
    private static final String STATS_RECENT_BLOCKED_ENDPOINT = "/stats/recent_blocked";
    private static final String STATS_TOP_DOMAINS_ENDPOINT = "/stats/top_domains";

    // POST endpoints
    private static final String DNS_BLOCKING_ENDPOINT = "/dns/blocking";

    private static final String QUERY_PARAM_SID = "sid";
    private static final String HEADER_X_FTL_SID = "X-FTL-SID";

    // ==================== Instance Fields ====================

    private final String ipAddress;
    private final int port;
    private final String scheme;
    private final String password;
    private final String apiBaseUrl;
    private final HttpClientUtil httpClient;
    private final Clock clock;

    private String sessionId;

    // ==================== Constructor ====================

    /**
     * Constructor that accepts a DnsBlockerConfig.
     * This is the primary constructor for production use.
     */
    public PiHoleHandler(DnsBlockerConfig config) {
        // IMPORTANT:
        // The handler must respect the passed config. Loading from ConfigurationService here
        // can silently override callers (e.g. if we later support multiple instances).
        this(config, new HttpClientUtil(), Clock.systemDefaultZone(), false, true);
    }

    /**
     * Legacy constructor for backward compatibility.
     * 
     * @deprecated Use {@link #PiHoleHandler(DnsBlockerConfig)} instead
     */
    @Deprecated
    public PiHoleHandler(String ipAddress, int port, String scheme, String password) {
        this(DnsBlockerConfig.forPiHole(ipAddress, port, scheme, password),
                new HttpClientUtil(), Clock.systemDefaultZone(), false, true);
    }

    /**
     * Internal constructor to allow unit testing with an injected HTTP client and
     * clock.
     */
    PiHoleHandler(DnsBlockerConfig config,
            HttpClientUtil httpClient,
            Clock clock,
            boolean loadFromConfiguration,
            boolean authenticateOnConstruct) {
        log("=== Initializing PiHoleHandler ===");
        log("Input params - IP: " + config.ipAddress() + ", Port: " + config.port() + ", Scheme: " + config.scheme());

        if (loadFromConfiguration) {
            // Load configuration from service
            ConfigurationService configService = new ConfigurationService();
            configService.readConfiguration();

            var loadedConfig = configService.getConfigDNS1();
            if (loadedConfig != null) {
                this.ipAddress = loadedConfig.getIPAddress();
                this.port = loadedConfig.getPort();
                this.scheme = loadedConfig.getScheme();
                this.password = loadedConfig.password();
            } else {
                this.ipAddress = config.ipAddress();
                this.port = config.port();
                this.scheme = config.scheme() != null ? config.scheme() : DnsBlockerConfig.DEFAULT_SCHEME;
                this.password = config.password();
            }
        } else {
            this.ipAddress = config.ipAddress();
            this.port = config.port();
            this.scheme = config.scheme() != null ? config.scheme() : "http";
            this.password = config.password();
        }

        this.apiBaseUrl = buildApiBaseUrl();
        this.httpClient = httpClient == null ? new HttpClientUtil() : httpClient;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;

        log("API Base URL: " + this.apiBaseUrl);
        log("Password configured: " + (this.password != null && !this.password.isBlank()));

        // Authenticate on construction
        if (authenticateOnConstruct) {
            authenticate();
        }
    }

    private String buildApiBaseUrl() {
        return scheme + "://" + ipAddress + ":" + port + API_PATH;
    }

    // ==================== Logging ====================

    private static void log(String message) {
        if (VERBOSE) {
            LOGGER.log(Level.FINE, () -> "[PiHole] " + message);
        }
    }

    private static void logInfo(String message) {
        LOGGER.log(Level.INFO, () -> message);
    }

    private static void logError(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }

    // ==================== Authentication ====================

    @Override
    public boolean authenticate() {
        log("=== authenticate() called ===");

        if (password == null || password.isBlank()) {
            log("Password is required for authentication");
            return false;
        }

        String url = apiBaseUrl + AUTH_ENDPOINT;
        log("Auth URL: " + url);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("password", password);

        try {
            HttpResponsePayload response = httpClient.postJson(url, requestBody, Collections.emptyMap());

            log("Response status code: " + response.statusCode());

            if (!response.isSuccessful()) {
                log("Authentication failed with HTTP " + response.statusCode());
                logInfo("Authentication failed: HTTP " + response.statusCode());
                return false;
            }

            parseAuthResponse(response);
            log("=== Authentication complete ===");
            return sessionId != null && !sessionId.isBlank();

        } catch (IOException e) {
            logError("Authentication failed with IOException", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Authentication interrupted", e);
            return false;
        }
    }

    private void parseAuthResponse(HttpResponsePayload response) {
        Optional<JsonNode> jsonOpt = response.bodyAsJson();

        if (jsonOpt.isEmpty()) {
            log("Failed to parse JSON response: " + response.bodyText());
            return;
        }

        JsonNode json = jsonOpt.get();
        log("Parsed JSON response successfully");

        if (!json.has("session")) {
            log("No 'session' object in response");
            return;
        }

        JsonNode session = json.get("session");

        if (session.has("sid") && !session.get("sid").isNull()) {
            this.sessionId = session.get("sid").asText();
            log("Session ID obtained: " + maskSessionId(sessionId));
            logInfo("Session ID: " + maskSessionId(sessionId));
        }

        if (session.has("valid")) {
            boolean isValid = session.get("valid").asBoolean();
            log("Session valid: " + isValid);
        }

        if (session.has("message")) {
            String message = session.get("message").asText();
            log("Session message: " + message);
        }
    }

    private String maskSessionId(String sid) {
        if (sid == null || sid.length() < 10) {
            return "***";
        }
        return sid.substring(0, 10) + "...";
    }

    // ==================== API Methods ====================

    /**
     * Retrieves Pi-hole statistics as raw JSON string.
     * Implements DnsBlockerHandler.getStats().
     */
    @Override
    public String getStats() {
        return getPiHoleStats();
    }

    /**
     * Retrieves Pi-hole statistics as raw JSON string.
     */
    public String getPiHoleStats() {
        log("=== getPiHoleStats() called ===");
        log("Session ID: " + maskSessionId(sessionId));

        try {
            HttpResponsePayload response = getApi(STATS_SUMMARY_ENDPOINT, Collections.emptyMap());
            if (!response.isSuccessful()) {
                log("Failed to get stats - HTTP " + response.statusCode());
                return "";
            }
            return response.bodyText();
        } catch (IOException e) {
            logError("IOException while fetching stats summary", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching stats summary", e);
        }
        return "";
    }

    /**
     * Retrieves the last blocked domain.
     */
    @Override
    public String getLastBlocked() {
        log("=== getLastBlocked() called ===");

        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("count", "1");
            HttpResponsePayload response = getApi(STATS_RECENT_BLOCKED_ENDPOINT, queryParams);

            if (!response.isSuccessful()) {
                log("Failed to get recent blocked - HTTP " + response.statusCode());
                return "";
            }

            Optional<JsonNode> jsonOpt = response.bodyAsJson();
            if (jsonOpt.isEmpty()) {
                log("Failed to parse recent blocked JSON response");
                return "";
            }

            JsonNode json = jsonOpt.get();
            JsonNode blocked = json.get("blocked");
            if (blocked != null && blocked.isArray() && blocked.size() > 0) {
                return blocked.get(0).asText("");
            }
            return "";

        } catch (IOException e) {
            logError("IOException while fetching recent blocked", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching recent blocked", e);
        }
        return "";
    }

    /**
     * Retrieves the Pi-hole version information.
     */
    @Override
    public String getVersion() {
        log("=== getVersion() called ===");

        String url = apiBaseUrl + VERSION_ENDPOINT;
        log("Version URL: " + url);

        try {
            Map<String, String> queryParams = authQueryParams();

            HttpResponsePayload response = httpClient.get(url, queryParams, Collections.emptyMap());

            log("Version response status: " + response.statusCode());

            if (!response.isSuccessful()) {
                log("Failed to get version - HTTP " + response.statusCode());
                return "";
            }

            return parseVersionResponse(response);

        } catch (IOException e) {
            logError("IOException while fetching version", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching version", e);
        }

        return "";
    }

    private String parseVersionResponse(HttpResponsePayload response) {
        Optional<JsonNode> jsonOpt = response.bodyAsJson();

        if (jsonOpt.isEmpty()) {
            log("Failed to parse version JSON response");
            return "";
        }

        JsonNode json = jsonOpt.get();
        log("Version JSON parsed successfully");

        if (!json.has("version")) {
            log("No 'version' key in response");
            return "";
        }

        JsonNode versionNode = json.get("version");

        // Try FTL version first
        String version = extractVersion(versionNode, "ftl");
        if (!version.isEmpty()) {
            log("Returning FTL version: " + version);
            return version;
        }

        // Fallback to core version
        version = extractVersion(versionNode, "core");
        if (!version.isEmpty()) {
            log("Returning core version: " + version);
            return version;
        }

        log("Could not extract version from response");
        return "";
    }

    private String extractVersion(JsonNode versionNode, String component) {
        if (versionNode.has(component)) {
            JsonNode componentNode = versionNode.get(component);
            if (componentNode.has("local")) {
                JsonNode localNode = componentNode.get("local");
                if (localNode.has("version")) {
                    return localNode.get("version").asText("");
                }
            }
        }
        return "";
    }

    /**
     * Retrieves top X blocked domains as raw JSON string.
     */
    @Override
    public String getTopXBlocked(int count) {
        log("=== getTopXBlocked(" + count + ") called ===");
        if (count <= 0) {
            return "";
        }

        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("blocked", "true");
            queryParams.put("count", String.valueOf(count));

            HttpResponsePayload response = getApi(STATS_TOP_DOMAINS_ENDPOINT, queryParams);

            if (!response.isSuccessful()) {
                log("Failed to get top blocked domains - HTTP " + response.statusCode());
                return "";
            }

            return response.bodyText();

        } catch (IOException e) {
            logError("IOException while fetching top blocked domains", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching top blocked domains", e);
        }

        return "";
    }

    /**
     * Retrieves the gravity last update time as a formatted string.
     */
    @Override
    public String getGravityLastUpdate() {
        log("=== getGravityLastUpdate() called ===");

        try {
            HttpResponsePayload response = getApi(STATS_SUMMARY_ENDPOINT, Collections.emptyMap());

            if (!response.isSuccessful()) {
                log("Failed to get stats for gravity last update - HTTP " + response.statusCode());
                return "";
            }

            Optional<JsonNode> jsonOpt = response.bodyAsJson();
            if (jsonOpt.isEmpty()) {
                log("Failed to parse stats JSON response");
                return "";
            }

            JsonNode gravity = jsonOpt.get().get("gravity");
            long lastUpdate = 0L;
            if (gravity != null && gravity.has("last_update")) {
                lastUpdate = gravity.get("last_update").asLong(0L);
            }

            if (lastUpdate <= 0L) {
                return "Gravity: unknown";
            }

            return formatRelativeEpochSeconds(lastUpdate);

        } catch (IOException e) {
            logError("IOException while fetching gravity last update", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching gravity last update", e);
        }
        return "";
    }

    /**
     * Changes current blocking status (Pi-hole v6+): POST /dns/blocking
     *
     * Request body:
     * - blocking: boolean (required)
     * - timer: number|null (optional) seconds until automatic reversal; null to
     * cancel any running timer
     *
     * Returns raw JSON response from Pi-hole, or empty string on failure.
     */
    @Override
    public String setDnsBlocking(boolean blocking, Integer timerSeconds) {
        log("=== setDnsBlocking(blocking=" + blocking + ", timerSeconds=" + timerSeconds + ") called ===");
        Map<String, Object> body = new HashMap<>();
        body.put("blocking", blocking);
        // If null -> explicitly send null so the server can clear timers
        body.put("timer", timerSeconds);

        try {
            HttpResponsePayload response = postApi(DNS_BLOCKING_ENDPOINT, body, Collections.emptyMap());
            if (!response.isSuccessful()) {
                log("Failed to set dns blocking - HTTP " + response.statusCode());
                return "";
            }
            return response.bodyText();
        } catch (IOException e) {
            logError("IOException while setting dns blocking", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while setting dns blocking", e);
        }
        return "";
    }

    /**
     * Retrieves current DNS blocking status (Pi-hole v6+): GET /dns/blocking
     *
     * Returns raw JSON response from Pi-hole, or empty string on failure.
     */
    @Override
    public String getDnsBlockingStatus() {
        log("=== getDnsBlockingStatus() called ===");
        try {
            HttpResponsePayload response = getApi(DNS_BLOCKING_ENDPOINT, Collections.emptyMap());
            if (!response.isSuccessful()) {
                log("Failed to get dns blocking status - HTTP " + response.statusCode());
                return "";
            }
            return response.bodyText();
        } catch (IOException e) {
            logError("IOException while fetching dns blocking status", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching dns blocking status", e);
        }
        return "";
    }

    // ==================== Internal Helpers ====================

    private Map<String, String> authQueryParams() {
        if (sessionId == null || sessionId.isBlank()) {
            return new HashMap<>();
        }
        Map<String, String> params = new HashMap<>();
        params.put(QUERY_PARAM_SID, sessionId);
        return params;
    }

    private HttpResponsePayload getApi(String endpoint, Map<String, String> extraQueryParams)
            throws IOException, InterruptedException {
        String url = apiBaseUrl + endpoint;
        Map<String, String> queryParams = authQueryParams();
        if (extraQueryParams != null && !extraQueryParams.isEmpty()) {
            queryParams.putAll(extraQueryParams);
        }
        return httpClient.get(url, queryParams, Collections.emptyMap());
    }

    /**
     * Generic POST helper for Pi-hole API endpoints.
     *
     * Applies authentication in two forms when available:
     * - query param: sid=...
     * - header: X-FTL-SID: ...
     *
     * This mirrors the API docs which allow either form.
     */
    private HttpResponsePayload postApi(String endpoint,
            Object jsonBody,
            Map<String, String> extraQueryParams)
            throws IOException, InterruptedException {
        // If we have a password but no session yet, try to authenticate lazily.
        if ((sessionId == null || sessionId.isBlank()) && password != null && !password.isBlank()) {
            authenticate();
        }

        String url = apiBaseUrl + endpoint;

        Map<String, String> queryParams = authQueryParams();
        if (extraQueryParams != null && !extraQueryParams.isEmpty()) {
            queryParams.putAll(extraQueryParams);
        }

        Map<String, String> headers = new HashMap<>();
        if (sessionId != null && !sessionId.isBlank()) {
            headers.put(HEADER_X_FTL_SID, sessionId);
        }

        return httpClient.postJson(url, jsonBody, queryParams, headers);
    }

    private String formatRelativeEpochSeconds(long epochSeconds) {
        Instant now = Instant.now(clock);
        Instant then = Instant.ofEpochSecond(epochSeconds);
        Duration d = Duration.between(then, now);
        if (d.isNegative()) {
            d = Duration.ZERO;
        }

        long days = d.toDays();
        long hours = d.toHours() - days * 24;
        long minutes = d.toMinutes() - (days * 24 * 60) - (hours * 60);

        if (days > 0) {
            return "Gravity: " + days + "d " + hours + "h ago";
        }
        if (hours > 0) {
            return "Gravity: " + hours + "h " + minutes + "m ago";
        }
        if (minutes > 0) {
            return "Gravity: " + minutes + "m ago";
        }
        return "Gravity: just now";
    }

    // ==================== Getters ====================

    public String getIPAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
