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

import domain.pihole.Gravity;
import domain.pihole.PiHole;
import domain.pihole.TopAd;
import helpers.HelperService;
import helpers.HttpClientUtil;
import helpers.HttpClientUtil.HttpResponsePayload;
import services.configuration.ConfigurationService;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PiHoleHandler {

    private final String IPAddress;
    private final int Port;
    private final String Scheme;
    private final String password;
    private final String apiBaseUrl;
    private String sessionId;
    private static final String API_PATH = "/api";
    private static final String AUTH_QUERY_PARAM = "/auth";
    
    // Enable verbose logging via system property: -Dpihole.verbose=true
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));
    
    private static void log(String message) {
        if (VERBOSE) {
            System.out.println("[PiHole] " + java.time.LocalDateTime.now() + " - " + message);
        }
    }

    public PiHoleHandler(String IPAddress, int Port, String Scheme, String password) {
        log("=== Initializing PiHoleHandler ===");
        log("Input params - IP: " + IPAddress + ", Port: " + Port + ", Scheme: " + Scheme + ", Password: " + (password != null ? "***" : "null"));
        
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.readConfiguration();
        
        this.IPAddress = configurationService.getConfigDNS1().getIPAddress();
        this.Port = configurationService.getConfigDNS1().getPort();
        this.Scheme = configurationService.getConfigDNS1().getScheme();
        this.password = configurationService.getConfigDNS1().getAUTH();
        this.apiBaseUrl = this.Scheme + "://" + this.IPAddress + ":" + this.Port + API_PATH;
        
        log("Loaded config - IP: " + this.IPAddress + ", Port: " + this.Port + ", Scheme: " + this.Scheme);
        log("API Base URL: " + this.apiBaseUrl);
        log("Password configured: " + (this.password != null && !this.password.isBlank()));
        
        try {
            log("Starting authentication...");
            this.authenticate();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            log("Authentication failed with IOException: " + e.getMessage());
            if (VERBOSE) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            System.out.println("InterruptedException: " + e.getMessage());
            log("Authentication failed with InterruptedException: " + e.getMessage());
        }
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getPort() {
        return Port;
    }

    public String getScheme() {
        return Scheme;
    }

    public String getSessionId() {
        return sessionId;
    }

    public PiHole getPiHoleStats() {
        log("=== getPiHoleStats() called ===");
        log("Session ID: " + (sessionId != null ? sessionId.substring(0, Math.min(10, sessionId.length())) + "..." : "null"));
    /*
        JsonNode jsonResult = getApiResponseAsJson("summary", "");
        if (jsonResult != null) {
            JsonNode gravity_json = jsonResult.get("gravity_last_updated");
            JsonNode relative_json = gravity_json.get("relative");

            boolean file_exists = gravity_json.get("file_exists").asBoolean();
            Long absolute = gravity_json.get("absolute").asLong();
            Long days = relative_json.get("days").asLong();
            Long hours = relative_json.get("hours").asLong();
            Long minutes = relative_json.get("minutes").asLong();

            Gravity gravity = new Gravity(file_exists, absolute, days, hours, minutes);
            try {

                Long domains_being_blocked = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("domains_being_blocked")));
                Long dns_queries_today = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("dns_queries_today")));
                Long ads_blocked_today = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("ads_blocked_today")));
                Double ads_percentage_today = Double.parseDouble(HelperService.convertJsonToLong(jsonResult.get("ads_percentage_today")));
                Long unique_domains = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("unique_domains")));
                Long queries_forwarded = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("queries_forwarded")));
                Long queries_cached = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("queries_cached")));
                Long clients_ever_seen = jsonResult.get("clients_ever_seen").asLong();
                Long unique_clients = jsonResult.get("unique_clients").asLong();
                Long dns_queries_all_types = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("dns_queries_all_types")));
                Long reply_NODATA = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("reply_NODATA")));
                Long reply_NXDOMAIN = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("reply_NXDOMAIN")));
                Long reply_CNAME = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("reply_CNAME")));
                Long reply_IP = Long.parseLong(HelperService.convertJsonToLong(jsonResult.get("reply_IP")));
                Long privacy_level = jsonResult.get("privacy_level").asLong();
                String status = jsonResult.get("status").asText();


                return new PiHole(domains_being_blocked, dns_queries_today, ads_blocked_today, ads_percentage_today, unique_domains, queries_forwarded, queries_cached, clients_ever_seen, unique_clients, dns_queries_all_types, reply_NODATA, reply_NXDOMAIN, reply_CNAME, reply_IP, privacy_level, status, gravity);
            } catch (NumberFormatException nfe) {
                System.out.println("NumberFormatException: " + nfe.getMessage());
                return null;
            }
        }*/
        log("getPiHoleStats() returning null (stats implementation pending)");
        return null;
    }

    public String getLastBlocked() {
        log("=== getLastBlocked() called ===");
        /*if (Auth != null && !Auth.isEmpty()) {
            HttpResponsePayload response = httpClientUtil.get(apiBaseUrl + "/api/recentBlocked");
            if (!response.isSuccessful()) {
                System.out.println("Failed : HTTP Error code : " + response.statusCode());
            }
            var jsonOpt = response.bodyAsJson();
            if (!jsonOpt.isPresent()) {
                System.out.println("Failed to parse JSON response: " + response.bodyText());
                return "";
            }
            if (output != null && output.equals("")) return "";

            return output;

        } else return "Please verify your Authentication Token";*/
        log("getLastBlocked() returning empty string (implementation pending)");
        return "";
    }

    public String getVersion() {
        log("=== getVersion() called ===");
        try {
            String url = apiBaseUrl + "/info/version";
            System.out.println("Version URL: " + url);

            System.out.println("sid: " + sessionId);
            if (sessionId == null || sessionId.isBlank()) {
                System.out.println("sessionId is required for authentication");
                return "";
            }

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("sid", sessionId);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");

            HttpClientUtil httpClientUtil = new HttpClientUtil();
            log("Sending GET request for version...");
            HttpResponsePayload response = httpClientUtil.get(url, requestBody, Collections.emptyMap());
            
            log("Version response status: " + response.statusCode());
            if (!response.isSuccessful()) {
                log("ERROR: Failed to get version - HTTP " + response.statusCode());
                System.out.println("Failed to get version: HTTP Error code: " + response.statusCode());
                return "";
            }
            
            var jsonOpt = response.bodyAsJson();
            if (jsonOpt.isEmpty()) {
                log("ERROR: Failed to parse version JSON response");
                log("Raw response: " + response.bodyText());
                System.out.println("Failed to parse version JSON response: " + response.bodyText());
                return "";
            }
            
            JsonNode json = jsonOpt.get();
            log("Version JSON parsed successfully");
            log("JSON structure: " + json.toString());
            
            if (json.has("version")) {
                JsonNode versionNode = json.get("version");
                log("Found 'version' node");
                
                // Return the FTL local version as the main Pi-hole version
                if (versionNode.has("ftl") && versionNode.get("ftl").has("local")) {
                    JsonNode ftlLocal = versionNode.get("ftl").get("local");
                    if (ftlLocal.has("version")) {
                        String version = ftlLocal.get("version").asText();
                        log("Returning FTL version: " + version);
                        return version;
                    }
                }
                // Fallback to core version if FTL not available
                if (versionNode.has("core") && versionNode.get("core").has("local")) {
                    JsonNode coreLocal = versionNode.get("core").get("local");
                    if (coreLocal.has("version")) {
                        String version = coreLocal.get("version").asText();
                        log("Returning core version: " + version);
                        return version;
                    }
                }
                log("WARNING: Could not extract version from response");
            } else {
                log("WARNING: No 'version' key in response");
            }
        } catch (IOException e) {
            log("ERROR: IOException while fetching version: " + e.getMessage());
            System.out.println("IOException while fetching version: " + e.getMessage());
            if (VERBOSE) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            log("ERROR: InterruptedException while fetching version: " + e.getMessage());
            System.out.println("InterruptedException while fetching version: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        log("getVersion() returning empty string");
        return "";
    }

    public List<TopAd> getTopXBlocked(int x) {
        log("=== getTopXBlocked(" + x + ") called ===");
        log("getTopXBlocked() returning null (implementation pending)");
        return null;
        /*if (Auth != null && !Auth.isEmpty()) {

            JsonNode jsonResult = getApiResponseAsJson("topItems", String.valueOf(x));
            if (jsonResult != null && !jsonResult.isEmpty()) {
                JsonNode topADS = jsonResult.get("top_ads");
                List<TopAd> list = new ArrayList<>();

                topADS.fields().forEachRemaining(entry -> 
                    list.add(new TopAd(entry.getKey(), entry.getValue().asLong()))
                );

                list.sort((s1, s2) -> Long.compare(s2.getNumberBlocked(), s1.getNumberBlocked()));

                return list;
            }
        } else return null;
        return null;*/
    }

    public String getGravityLastUpdate() {
        log("=== getGravityLastUpdate() called ===");

        PiHole pihole1 = getPiHoleStats();
        if (pihole1 != null) {
            log("Got PiHole stats, extracting gravity info...");
            String textToDisplay = "";
            long days = pihole1.getGravity().getDays();
            if (days <= 1) textToDisplay += days + " day";
            else textToDisplay += days + " days";

            long hours = pihole1.getGravity().getHours();
            if (hours <= 1) textToDisplay += " " + hours + " hour";
            else textToDisplay += " " + hours + " hours";

            long mins = pihole1.getGravity().getMinutes();
            if (mins <= 1) textToDisplay += " " + mins + " min";
            else textToDisplay += " " + mins + " mins";

            log("Gravity last update: " + textToDisplay);
            return textToDisplay;
        }
        log("PiHole stats is null, returning empty string");
        return "";
    }

    private void authenticate() throws IOException, InterruptedException {
        log("=== authenticate() called ===");
        log("Password present: " + (password != null && !password.isBlank()));
        
        if (password == null || password.isBlank()) {
            log("ERROR: Password is required for authentication");
            System.out.println("Password is required for authentication");
            return;
        }
        
        String url = apiBaseUrl + AUTH_QUERY_PARAM;
        log("Auth URL: " + url);
        
        // Create request body with password
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("password", password);
        log("Request body prepared (password masked)");
        
        // Send POST request with JSON body
        HttpClientUtil httpClientUtil = new HttpClientUtil();
        log("Sending POST request to " + url);
        HttpResponsePayload response = httpClientUtil.postJson(url, requestBody, Collections.emptyMap());
        
        log("Response status code: " + response.statusCode());
        log("Response successful: " + response.isSuccessful());
        
        if (!response.isSuccessful()) {
            log("ERROR: Authentication failed with HTTP " + response.statusCode());
            log("Response body: " + response.bodyText());
            System.out.println("Failed : HTTP Error code : " + response.statusCode());
            System.out.println("Response body: " + response.bodyText());
            return;
        }
        
        var jsonOpt = response.bodyAsJson();
        if (!jsonOpt.isPresent()) {
            log("ERROR: Failed to parse JSON response");
            log("Raw response: " + response.bodyText());
            System.out.println("Failed to parse JSON response: " + response.bodyText());
            return;
        }
        
        JsonNode json = jsonOpt.get();
        log("Parsed JSON response successfully");
        log("JSON structure: " + json.toString());
        
        // Parse nested session object
        if (json.has("session")) {
            JsonNode session = json.get("session");
            log("Found 'session' object in response");
            
            if (session.has("sid") && !session.get("sid").isNull()) {
                this.sessionId = session.get("sid").asText();
                log("Session ID obtained: " + this.sessionId.substring(0, Math.min(10, this.sessionId.length())) + "...");
                System.out.println("Session ID: " + this.sessionId);
            } else {
                log("WARNING: No 'sid' field in session object");
            }
            
            if (session.has("valid")) {
                boolean isValid = session.get("valid").asBoolean();
                log("Session valid: " + isValid);
                System.out.println("Session valid: " + isValid);
            }
            
            if (session.has("message")) {
                String message = session.get("message").asText();
                log("Session message: " + message);
                System.out.println("Session message: " + message);
            }
        } else {
            log("WARNING: No 'session' object in response. Available keys: " + json.fieldNames());
        }
        
        log("=== Authentication complete ===");
    }

}
