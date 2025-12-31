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

package services.adguard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import domain.configuration.DnsBlockerConfig;
import helpers.HttpClientUtil;
import helpers.HttpClientUtil.HttpResponsePayload;
import services.DnsBlockerHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for AdGuard Home API communication.
 * Implements the DnsBlockerHandler interface for platform abstraction.
 * Uses HTTP Basic Authentication instead of session-based auth.
 */
public class AdGuardHomeHandler implements DnsBlockerHandler {

    // ==================== Constants ====================

    private static final Logger LOGGER = Logger.getLogger(AdGuardHomeHandler.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("dnsbloquer.verbose", "false"));

    private static final String API_PATH = "/control";
    private static final String STATUS_ENDPOINT = "/status";
    private static final String STATS_ENDPOINT = "/stats";
    private static final String QUERYLOG_ENDPOINT = "/querylog";
    private static final String DNS_INFO_ENDPOINT = "/dns_info";
    private static final String DNS_CONFIG_ENDPOINT = "/dns_config";
    private static final String FILTERING_STATUS_ENDPOINT = "/filtering/status";

    // Generic (platform-agnostic) JSON schemas returned to the UI layer.
    // The widget should not be coupled to Pi-hole field names.
    private static final String SCHEMA_STATS_V1 = "dnsblocker.stats.v1";
    private static final String SCHEMA_TOP_BLOCKED_V1 = "dnsblocker.top_blocked.v1";
    private static final String SCHEMA_BLOCKING_STATUS_V1 = "dnsblocker.blocking_status.v1";

    // ==================== Instance Fields ====================

    private final String ipAddress;
    private final int port;
    private final String scheme;
    private final String username;
    private final String password;
    private final String apiBaseUrl;
    private final HttpClientUtil httpClient;
    private final ObjectMapper objectMapper;

    // Cached auth header to avoid recalculating on every request
    private final String basicAuthHeader;

    // ==================== Constructor ====================

    /**
     * Constructor that accepts a DnsBlockerConfig.
     */
    public AdGuardHomeHandler(DnsBlockerConfig config) {
        this(config, new HttpClientUtil());
    }

    /**
     * Internal constructor for testing with injected HTTP client.
     */
    AdGuardHomeHandler(DnsBlockerConfig config, HttpClientUtil httpClient) {
        log("=== Initializing AdGuardHomeHandler ===");
        log("Input params - IP: " + config.ipAddress() + ", Port: " + config.port() +
                ", Scheme: " + config.scheme());

        this.ipAddress = config.ipAddress();
        this.port = config.port();
        this.scheme = config.scheme() != null ? config.scheme() : "http";
        this.username = config.username() != null ? config.username() : "";
        this.password = config.password() != null ? config.password() : "";
        this.apiBaseUrl = buildApiBaseUrl();
        this.httpClient = httpClient != null ? httpClient : new HttpClientUtil();
        this.objectMapper = new ObjectMapper();

        // Pre-calculate Basic Auth header
        this.basicAuthHeader = createBasicAuthHeader();

        log("API Base URL: " + this.apiBaseUrl);
        log("Username configured: " + !this.username.isBlank());
        log("Password configured: " + !this.password.isBlank());
    }

    private String buildApiBaseUrl() {
        return scheme + "://" + ipAddress + ":" + port + API_PATH;
    }

    private String createBasicAuthHeader() {
        if (username.isBlank() || password.isBlank()) {
            return "";
        }
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    // ==================== Logging ====================

    private static void log(String message) {
        if (VERBOSE) {
            LOGGER.log(Level.FINE, () -> "[AdGuardHome] " + message);
        }
    }

    private static void logError(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }

    // ==================== DnsBlockerHandler Implementation ====================

    @Override
    public boolean authenticate() {
        log("=== authenticate() called ===");

        if (username.isBlank() || password.isBlank()) {
            log("Username and password are required for authentication");
            return false;
        }

        // For AdGuard Home, we just test the credentials by calling /status
        try {
            HttpResponsePayload response = getApi(STATUS_ENDPOINT, Collections.emptyMap());

            if (!response.isSuccessful()) {
                log("Authentication test failed with HTTP " + response.statusCode());
                return false;
            }

            log("Authentication successful");
            return true;

        } catch (IOException e) {
            logError("Authentication failed with IOException", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Authentication interrupted", e);
            return false;
        }
    }

    @Override
    public String getStats() {
        log("=== getStats() called ===");

        try {
            HttpResponsePayload response = getApi(STATS_ENDPOINT, Collections.emptyMap());
            if (!response.isSuccessful()) {
                log("Failed to get stats - HTTP " + response.statusCode());
                return "";
            }

            // Transform AdGuard Home stats to a generic schema understood by the widget
            return transformStatsToGeneric(response.bodyText());

        } catch (IOException e) {
            logError("IOException while fetching stats", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching stats", e);
        }
        return "";
    }

    /**
     * Transforms AdGuard Home stats JSON to a platform-agnostic schema:
     *
     * {
     *   "schema": "dnsblocker.stats.v1",
     *   "queries": { "total": <long>, "blocked": <long>, "percent_blocked": <double> },
     *   "blocklist": { "size": <long> },
     *   "blocking": { "enabled": <boolean|null> },
     *   "source": "adguard-home"
     * }
     */
    private String transformStatsToGeneric(String adGuardJson) {
        try {
            JsonNode agNode = objectMapper.readTree(adGuardJson);

            long total = agNode.has("num_dns_queries") ? agNode.get("num_dns_queries").asLong(0L) : 0L;
            long blocked = agNode.has("num_blocked_filtering") ? agNode.get("num_blocked_filtering").asLong(0L) : 0L;
            double percentage = total > 0 ? (blocked * 100.0 / total) : 0.0;

            long blocklistSize = Math.max(0L, (long) getEnabledFiltersCount());

            ObjectNode out = objectMapper.createObjectNode();
            out.put("schema", SCHEMA_STATS_V1);
            out.put("source", "adguard-home");

            ObjectNode queries = objectMapper.createObjectNode();
            queries.put("total", Math.max(0L, total));
            queries.put("blocked", Math.max(0L, blocked));
            queries.put("percent_blocked", Math.max(0.0, percentage));
            out.set("queries", queries);

            ObjectNode blocklist = objectMapper.createObjectNode();
            blocklist.put("size", blocklistSize);
            out.set("blocklist", blocklist);

            // We intentionally do not hardcode blocking state here; the dedicated status endpoint
            // is preferred. When unavailable, the widget can fall back to parsing other payloads.
            ObjectNode blocking = objectMapper.createObjectNode();
            blocking.putNull("enabled");
            out.set("blocking", blocking);

            return objectMapper.writeValueAsString(out);

        } catch (Exception e) {
            logError("Failed to transform AdGuard Home stats to generic schema", e);
            return adGuardJson; // Return original on parse error (widget has legacy fallbacks)
        }
    }

    @Override
    public String getLastBlocked() {
        log("=== getLastBlocked() called ===");

        try {
            // Query log with filter for blocked items
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("response_status", "filtered");
            queryParams.put("limit", "1");

            HttpResponsePayload response = getApi(QUERYLOG_ENDPOINT, queryParams);

            if (!response.isSuccessful()) {
                log("Failed to get query log - HTTP " + response.statusCode());
                return "";
            }

            Optional<JsonNode> jsonOpt = response.bodyAsJson();
            if (jsonOpt.isEmpty()) {
                log("Failed to parse query log JSON response");
                return "";
            }

            JsonNode json = jsonOpt.get();
            JsonNode data = json.get("data");
            if (data != null && data.isArray() && data.size() > 0) {
                JsonNode firstEntry = data.get(0);
                if (firstEntry.has("question")) {
                    JsonNode question = firstEntry.get("question");
                    if (question.has("name")) {
                        return question.get("name").asText("");
                    }
                }
            }
            return "";

        } catch (IOException e) {
            logError("IOException while fetching last blocked", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching last blocked", e);
        }
        return "";
    }

    @Override
    public String getVersion() {
        log("=== getVersion() called ===");

        try {
            HttpResponsePayload response = getApi(STATUS_ENDPOINT, Collections.emptyMap());

            if (!response.isSuccessful()) {
                log("Failed to get status for version - HTTP " + response.statusCode());
                return "";
            }

            Optional<JsonNode> jsonOpt = response.bodyAsJson();
            if (jsonOpt.isEmpty()) {
                log("Failed to parse status JSON response");
                return "";
            }

            JsonNode json = jsonOpt.get();
            if (json.has("version")) {
                return json.get("version").asText("");
            }

            return "";

        } catch (IOException e) {
            logError("IOException while fetching version", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching version", e);
        }
        return "";
    }

    @Override
    public String getTopXBlocked(int count) {
        log("=== getTopXBlocked(" + count + ") called ===");
        if (count <= 0) {
            return "";
        }

        try {
            HttpResponsePayload response = getApi(STATS_ENDPOINT, Collections.emptyMap());

            if (!response.isSuccessful()) {
                log("Failed to get stats for top blocked - HTTP " + response.statusCode());
                return "";
            }

            Optional<JsonNode> jsonOpt = response.bodyAsJson();
            if (jsonOpt.isEmpty()) {
                log("Failed to parse stats JSON response");
                return "";
            }

            JsonNode json = jsonOpt.get();
            // AdGuard Home returns top_blocked_domains array
            if (json.has("top_blocked_domains")) {
                log("Found top_blocked_domains in response");
                JsonNode topBlocked = json.get("top_blocked_domains");
                return formatTopBlocked(topBlocked, count);
            }

            // Also try blocked_filtering which might have the data
            if (json.has("blocked_filtering")) {
                log("Found blocked_filtering in response");
                JsonNode blockedFiltering = json.get("blocked_filtering");
                return formatTopBlocked(blockedFiltering, count);
            }

            log("No top_blocked_domains found in stats response");
            return "";

        } catch (Exception e) {
            logError("Exception while fetching top blocked domains", e);
        }

        return "";
    }

    /**
     * Formats top blocked domains data into a platform-agnostic schema:
     *
     * { "schema": "dnsblocker.top_blocked.v1", "domains": [ { "domain": "...", "count": 123 } ] }
     */
    private String formatTopBlocked(JsonNode topBlocked, int count) {
        try {
            log("=== formatTopBlocked(count=" + count + ") ===");

            ObjectNode out = objectMapper.createObjectNode();
            out.put("schema", SCHEMA_TOP_BLOCKED_V1);
            out.put("source", "adguard-home");
            ArrayNode domains = objectMapper.createArrayNode();

            int added = 0;
            if (topBlocked.isArray()) {
                for (JsonNode entry : topBlocked) {
                    if (added >= count)
                        break;

                    String domain = "";
                    int hits = 0;

                    // AdGuard format: object with single key-value pair {"domain.com": 123}
                    if (entry.isObject()) {
                        var fields = entry.fields();
                        if (fields.hasNext()) {
                            var field = fields.next();
                            domain = field.getKey();
                            hits = field.getValue().asInt();
                        }
                    }
                    // Also support array format [domain, count] for compatibility
                    else if (entry.isArray() && entry.size() >= 2) {
                        domain = entry.get(0).asText();
                        hits = entry.get(1).asInt();
                    }

                    if (!domain.isEmpty()) {
                        ObjectNode item = objectMapper.createObjectNode();
                        item.put("domain", domain);
                        item.put("count", Math.max(0, hits));
                        domains.add(item);
                        added++;
                    }
                }
            }

            out.set("domains", domains);
            String result = objectMapper.writeValueAsString(out);
            return result;

        } catch (Exception e) {
            logError("Failed to format top blocked domains", e);
            return "";
        }
    }

    @Override
    public String getGravityLastUpdate() {
        log("=== getGravityLastUpdate() called ===");

        try {
            HttpResponsePayload response = getApi(FILTERING_STATUS_ENDPOINT, Collections.emptyMap());

            if (!response.isSuccessful()) {
                log("Failed to get filtering status - HTTP " + response.statusCode());
                return "";
            }

            Optional<JsonNode> jsonOpt = response.bodyAsJson();
            if (jsonOpt.isEmpty()) {
                log("Failed to parse filtering status JSON response");
                return "";
            }

            JsonNode json = jsonOpt.get();
            // Get the most recent filter update time
            if (json.has("filters")) {
                JsonNode filters = json.get("filters");
                String mostRecentUpdate = null;

                // Find the most recent last_updated timestamp
                if (filters.isArray()) {
                    for (JsonNode filter : filters) {
                        if (filter.has("last_updated") && filter.get("enabled").asBoolean(false)) {
                            String timestamp = filter.get("last_updated").asText();
                            if (mostRecentUpdate == null || timestamp.compareTo(mostRecentUpdate) > 0) {
                                mostRecentUpdate = timestamp;
                            }
                        }
                    }
                }

                if (mostRecentUpdate != null && !mostRecentUpdate.isEmpty()) {
                    return formatFilterUpdateTime(mostRecentUpdate);
                }
            }

            return "Filters: unknown";

        } catch (IOException e) {
            logError("IOException while fetching filter update time", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Interrupted while fetching filter update time", e);
        }
        return "";
    }

    /**
     * Gets the count of enabled filters from /filtering/status.
     * 
     * @return count of enabled filters
     */
    private int getEnabledFiltersCount() {
        try {
            HttpResponsePayload response = getApi(FILTERING_STATUS_ENDPOINT, Collections.emptyMap());

            if (!response.isSuccessful()) {
                log("Failed to get filtering status for count - HTTP " + response.statusCode());
                return 0;
            }

            Optional<JsonNode> jsonOpt = response.bodyAsJson();
            if (jsonOpt.isEmpty()) {
                return 0;
            }

            JsonNode json = jsonOpt.get();
            if (json.has("filters")) {
                JsonNode filters = json.get("filters");
                int totalRules = 0;

                if (filters.isArray()) {
                    for (JsonNode filter : filters) {
                        if (filter.has("enabled") && filter.get("enabled").asBoolean(false)) {
                            // Sum up the rules_count from each enabled filter
                            if (filter.has("rules_count")) {
                                totalRules += filter.get("rules_count").asInt(0);
                            }
                        }
                    }
                }

                log("Total blocked domains from enabled filters: " + totalRules);
                return totalRules;
            }

            return 0;
        } catch (Exception e) {
            logError("Exception while fetching filter count", e);
            return 0;
        }
    }

    private String formatFilterUpdateTime(String timestamp) {
        try {
            // Parse ISO 8601 timestamp from AdGuard Home
            Instant then = Instant.parse(timestamp);
            Instant now = Instant.now();
            Duration d = Duration.between(then, now);
            if (d.isNegative()) {
                d = Duration.ZERO;
            }

            long days = d.toDays();
            long hours = d.toHours() - days * 24;
            long minutes = d.toMinutes() - (days * 24 * 60) - (hours * 60);

            if (days > 0) {
                return "Filters: " + days + "d " + hours + "h ago";
            }
            if (hours > 0) {
                return "Filters: " + hours + "h " + minutes + "m ago";
            }
            if (minutes > 0) {
                return "Filters: " + minutes + "m ago";
            }
            return "Filters: just now";

        } catch (Exception e) {
            logError("Failed to parse filter update timestamp: " + timestamp, e);
            return "Filters: unknown";
        }
    }

    @Override
    public String setDnsBlocking(boolean blocking, Integer timerSeconds) {
        log("=== setDnsBlocking(blocking=" + blocking + ", timerSeconds=" + timerSeconds + ") called ===");

        try {
            // First get current DNS config
            HttpResponsePayload getResponse = getApi(DNS_INFO_ENDPOINT, Collections.emptyMap());
            if (!getResponse.isSuccessful()) {
                log("Failed to get DNS config - HTTP " + getResponse.statusCode());
                return "";
            }

            Optional<JsonNode> configOpt = getResponse.bodyAsJson();
            if (configOpt.isEmpty()) {
                return "";
            }

            JsonNode currentConfig = configOpt.get();

            // Create modified config with updated protection_enabled flag
            ObjectNode updatedConfig = currentConfig.deepCopy();
            updatedConfig.put("protection_enabled", blocking);

            // Note: AdGuard Home doesn't have a timer feature like Pi-hole
            // We ignore the timerSeconds parameter

            HttpResponsePayload response = postApi(DNS_CONFIG_ENDPOINT, updatedConfig, Collections.emptyMap());
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

    @Override
    public String getDnsBlockingStatus() {
        log("=== getDnsBlockingStatus() called ===");

        try {
            HttpResponsePayload response = getApi(STATUS_ENDPOINT, Collections.emptyMap());
            if (!response.isSuccessful()) {
                log("Failed to get status - HTTP " + response.statusCode());
                return "";
            }

            // Transform to a generic format that the widget can parse without Pi-hole coupling
            Optional<JsonNode> jsonOpt = response.bodyAsJson();
            if (jsonOpt.isPresent()) {
                JsonNode agStatus = jsonOpt.get();
                ObjectNode out = objectMapper.createObjectNode();
                out.put("schema", SCHEMA_BLOCKING_STATUS_V1);
                out.put("source", "adguard-home");

                ObjectNode blocking = objectMapper.createObjectNode();
                if (agStatus.has("protection_enabled")) {
                    blocking.put("enabled", agStatus.get("protection_enabled").asBoolean());
                } else {
                    blocking.putNull("enabled");
                }
                out.set("blocking", blocking);

                return objectMapper.writeValueAsString(out);
            }

            return response.bodyText();

        } catch (Exception e) {
            logError("Exception while fetching dns blocking status", e);
        }
        return "";
    }

    // ==================== Internal Helpers ====================

    private Map<String, String> authHeaders() {
        if (basicAuthHeader.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", basicAuthHeader);
        return headers;
    }

    private HttpResponsePayload getApi(String endpoint, Map<String, String> extraQueryParams)
            throws IOException, InterruptedException {
        String url = apiBaseUrl + endpoint;
        Map<String, String> headers = authHeaders();
        return httpClient.get(url, extraQueryParams, headers);
    }

    private HttpResponsePayload postApi(String endpoint,
            Object jsonBody,
            Map<String, String> extraQueryParams)
            throws IOException, InterruptedException {
        String url = apiBaseUrl + endpoint;
        Map<String, String> headers = authHeaders();
        return httpClient.postJson(url, jsonBody, extraQueryParams, headers);
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

    public String getUsername() {
        return username;
    }
}
