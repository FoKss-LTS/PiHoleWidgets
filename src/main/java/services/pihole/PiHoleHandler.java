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

    public PiHoleHandler(String IPAddress, int Port, String Scheme, String password) {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.readConfiguration();
        this.IPAddress = configurationService.getConfigDNS1().getIPAddress();
        this.Port = configurationService.getConfigDNS1().getPort();
        this.Scheme = configurationService.getConfigDNS1().getScheme();
        this.password = configurationService.getConfigDNS1().getAUTH();
        this.apiBaseUrl = this.Scheme + "://" + this.IPAddress + ":" + this.Port + API_PATH;
        try {
            this.authenticate();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("InterruptedException: " + e.getMessage());
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
        return null;
    }

    public String getLastBlocked() {
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
        return "";
    }

    public String getVersion() {

        //JsonNode jsonResult = getApiResponseAsJson("type%20&%20version", "");
        //if (jsonResult != null) return jsonResult.get("version").asText();
        return "";
    }

    public List<TopAd> getTopXBlocked(int x) {
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

        PiHole pihole1 = getPiHoleStats();
        if (pihole1 != null) {
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

            return textToDisplay;
        }
        return "";
    }

    private void authenticate() throws IOException, InterruptedException {
        System.out.println("password: " + password);
        if (password == null || password.isBlank()) {
            System.out.println("Password is required for authentication");
            return;
        }
        
        String url = apiBaseUrl + AUTH_QUERY_PARAM;
        System.out.println("url: " + url);
        
        // Create request body with password
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("password", password);
        
        // Send POST request with JSON body
        HttpClientUtil httpClientUtil = new HttpClientUtil();
        HttpResponsePayload response = httpClientUtil.postJson(url, requestBody, Collections.emptyMap());
        
        if (!response.isSuccessful()) {
            System.out.println("Failed : HTTP Error code : " + response.statusCode());
            System.out.println("Response body: " + response.bodyText());
            return;
        }
        
        var jsonOpt = response.bodyAsJson();
        if (!jsonOpt.isPresent()) {
            System.out.println("Failed to parse JSON response: " + response.bodyText());
            return;
        }
        
        JsonNode json = jsonOpt.get();
        System.out.println("json: " + json);
        
        // Parse nested session object
        if (json.has("session")) {
            JsonNode session = json.get("session");
            if (session.has("sid") && !session.get("sid").isNull()) {
                this.sessionId = session.get("sid").asText();
                System.out.println("Session ID: " + this.sessionId);
            }
            if (session.has("valid")) {
                boolean isValid = session.get("valid").asBoolean();
                System.out.println("Session valid: " + isValid);
            }
            if (session.has("message")) {
                String message = session.get("message").asText();
                System.out.println("Session message: " + message);
            }
        }
    }

}
