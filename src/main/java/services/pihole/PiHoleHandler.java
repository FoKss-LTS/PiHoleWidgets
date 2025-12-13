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

package services.pihole;

import com.fasterxml.jackson.databind.JsonNode;
import helpers.HttpClientUtil;
import helpers.HttpClientUtil.HttpResponsePayload;
import services.configuration.ConfigurationService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for Pi-hole API communication.
 * Manages authentication and data retrieval from Pi-hole servers.
 */
public class PiHoleHandler {

    // ==================== Constants ====================
    
    private static final Logger LOGGER = Logger.getLogger(PiHoleHandler.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));
    
    private static final String API_PATH = "/api";
    private static final String AUTH_ENDPOINT = "/auth";
    private static final String VERSION_ENDPOINT = "/info/version";
    
    // ==================== Instance Fields ====================
    
    private final String ipAddress;
    private final int port;
    private final String scheme;
    private final String password;
    private final String apiBaseUrl;
    private final HttpClientUtil httpClient;
    
    private String sessionId;

    // ==================== Constructor ====================

    public PiHoleHandler(String ipAddress, int port, String scheme, String password) {
        log("=== Initializing PiHoleHandler ===");
        log("Input params - IP: " + ipAddress + ", Port: " + port + ", Scheme: " + scheme);
        
        // Load configuration from service
        ConfigurationService configService = new ConfigurationService();
        configService.readConfiguration();
        
        var config = configService.getConfigDNS1();
        if (config != null) {
            this.ipAddress = config.getIPAddress();
            this.port = config.getPort();
            this.scheme = config.getScheme();
            this.password = config.getAUTH();
        } else {
            this.ipAddress = ipAddress;
            this.port = port;
            this.scheme = scheme != null ? scheme : "http";
            this.password = password;
        }
        
        this.apiBaseUrl = buildApiBaseUrl();
        this.httpClient = new HttpClientUtil();
        
        log("API Base URL: " + this.apiBaseUrl);
        log("Password configured: " + (this.password != null && !this.password.isBlank()));
        
        // Authenticate on construction
        authenticate();
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

    private void authenticate() {
        log("=== authenticate() called ===");
        
        if (password == null || password.isBlank()) {
            log("Password is required for authentication");
            return;
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
                return;
            }
            
            parseAuthResponse(response);
            
        } catch (IOException e) {
            logError("Authentication failed with IOException", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Authentication interrupted", e);
        }
        
        log("=== Authentication complete ===");
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
     * Note: Implementation pending - returns empty string.
     */
    public String getPiHoleStats() {
        log("=== getPiHoleStats() called ===");
        log("Session ID: " + maskSessionId(sessionId));
        
        // TODO: Implement stats retrieval using new Pi-hole API
        log("getPiHoleStats() returning empty string (implementation pending)");
        return "";
    }

    /**
     * Retrieves the last blocked domain.
     * Note: Implementation pending - returns empty string.
     */
    public String getLastBlocked() {
        log("=== getLastBlocked() called ===");
        
        // TODO: Implement last blocked retrieval using new Pi-hole API
        log("getLastBlocked() returning empty string (implementation pending)");
        return "";
    }

    /**
     * Retrieves the Pi-hole version information.
     */
    public String getVersion() {
        log("=== getVersion() called ===");
        
        if (sessionId == null || sessionId.isBlank()) {
            log("Session ID is required for authentication");
            logInfo("sessionId is required for authentication");
            return "";
        }
        
        String url = apiBaseUrl + VERSION_ENDPOINT;
        log("Version URL: " + url);
        
        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("sid", sessionId);
            
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
     * Note: Implementation pending - returns empty string.
     */
    public String getTopXBlocked(int count) {
        log("=== getTopXBlocked(" + count + ") called ===");
        
        // TODO: Implement top blocked retrieval using new Pi-hole API
        log("getTopXBlocked() returning empty string (implementation pending)");
        return "";
    }

    /**
     * Retrieves the gravity last update time as a formatted string.
     * Note: Implementation pending - returns empty string.
     */
    public String getGravityLastUpdate() {
        log("=== getGravityLastUpdate() called ===");
        
        // TODO: Implement gravity last update retrieval using new Pi-hole API
        log("getGravityLastUpdate() returning empty string (implementation pending)");
        return "";
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
