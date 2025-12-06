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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import services.helpers.HelperService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PiHoleHandler {

    private final String IPAddress;
    private final int Port;
    private final String Auth;


    public PiHoleHandler(String IPAddress, int Port, String auth) {
        this.IPAddress = IPAddress;
        this.Port = Port;
        this.Auth = auth;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public PiHole getPiHoleStats() {

        String output = getApiResponseAsString("summary", "");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResult = null;
        if (output != null && output.equals("")) return null;

        // Transform Raw result to JSON

        try {
            jsonResult = mapper.readTree(output);
        } catch (JsonProcessingException ioe) {
            ioe.printStackTrace();
        }

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
        }
        return null;
    }

    public String getLastBlocked() {
        if (Auth != null && !Auth.isEmpty()) {
            String output = getApiResponseAsString("recentBlocked", "");
            if (output != null && output.equals("")) return "";

            return output;

        } else return "Please verify your Authentication Token";
    }

    public String getVersion() {

        String output = getApiResponseAsString("type%20&%20version", "");
        if (output != null && output.equals("")) return "";

        // Transform Raw result to JSON

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResult = null;
        try {
            jsonResult = mapper.readTree(output);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (jsonResult != null) return jsonResult.get("version").asText();
        return "";
    }

    public List<TopAd> getTopXBlocked(int x) {
        if (Auth != null && !Auth.isEmpty()) {

            String output = getApiResponseAsString("topItems", String.valueOf(x));

            if (output != null && output.equals("")) return null;

            // Transform Raw result to JSON

            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode jsonResult = mapper.readTree(output);
                JsonNode topADS = jsonResult.get("top_ads");
                List<TopAd> list = new ArrayList<>();

                topADS.fields().forEachRemaining(entry -> 
                    list.add(new TopAd(entry.getKey(), entry.getValue().asLong()))
                );

                list.sort((s1, s2) -> Long.compare(s2.getNumberBlocked(), s1.getNumberBlocked()));

                return list;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        } else return null;
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


    private String getApiResponseAsString(String Param, String ParamVal) {

        String fullAuth = "";
        String fullParam = "";
        String fullParamVal = "";
        InputStreamReader in;
        BufferedReader br;

        int responseCode;

        if (!Param.isEmpty()) fullParam = "?" + Param;


        if (!ParamVal.isEmpty()) fullParamVal = "=" + ParamVal;

        if (Auth != null && !Auth.isEmpty()) fullAuth = "&auth=" + this.Auth;

        HttpURLConnection conn=null;
        if (!IPAddress.isEmpty()) {
            try {
                URL url = URI.create("http://" + IPAddress + ":" + Port + "/admin/api.php" + fullParam + fullParamVal + fullAuth).toURL();

                conn = (HttpURLConnection) url.openConnection();


                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                responseCode = conn.getResponseCode();


                System.out.println(conn.getResponseMessage());
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String server_response;
                    in = new InputStreamReader(conn.getInputStream());
                    br = new BufferedReader(in);

                    server_response = br.readLine();
                    return server_response;
                } else {
                    System.out.println("Failed : HTTP Error code : " + responseCode);
                    conn.disconnect();
                    return "";
                }

            } catch (SocketTimeoutException et) {
            System.out.println("Timed out: Can't login to API: Check your PiHole server ?");
            } catch (IOException e) {
                System.out.println("API Error for:" + IPAddress + " " + e.getMessage());
                return "";
            }
            finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        return "";
    }

}
