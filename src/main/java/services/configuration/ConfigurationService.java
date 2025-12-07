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

package services.configuration;

import domain.configuration.PiholeConfig;
import domain.configuration.WidgetConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import services.helpers.HelperService;

import java.io.File;
import java.io.IOException;

public class ConfigurationService {

    private File SETTTINGS_FILE = null;

    private final String folder_name = "Pihole Widget";
    private final String file_name = "settings.json";
    private final String home = System.getProperty("user.home");
    private final String file_path = home + "/" + folder_name + "/" + file_name;

    private WidgetConfig widgetConfig=null;

    private PiholeConfig configDNS1;
    private PiholeConfig configDNS2;

    public void readConfiguration() {

        SETTTINGS_FILE = new java.io.File(file_path);
        if (SETTTINGS_FILE == null || (SETTTINGS_FILE != null && !SETTTINGS_FILE.exists()))
            saveEmptyConfiguration();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonObject = mapper.readTree(SETTTINGS_FILE);

            JsonNode jsonWidget = jsonObject.get("Widget");
            JsonNode jsonDNS1 = jsonObject.get("DNS1");
            JsonNode jsonDNS2 = jsonObject.get("DNS2");

            Long port1 = jsonDNS1.has("Port") && !jsonDNS1.get("Port").isNull() ? jsonDNS1.get("Port").asLong() : 80L;
            Long port2 = jsonDNS2.has("Port") && !jsonDNS2.get("Port").isNull() ? jsonDNS2.get("Port").asLong() : 80L;
            String scheme1 = jsonDNS1.has("Scheme") && !jsonDNS1.get("Scheme").isNull() ? jsonDNS1.get("Scheme").asText() : "http";
            String scheme2 = jsonDNS2.has("Scheme") && !jsonDNS2.get("Scheme").isNull() ? jsonDNS2.get("Scheme").asText() : "http";

            configDNS1 = new PiholeConfig(jsonDNS1.get("IP").asText(), Math.toIntExact(port1), scheme1, jsonDNS1.get("Authentication Token").asText());
            configDNS2 = new PiholeConfig(jsonDNS2.get("IP").asText(), Math.toIntExact(port2), scheme2, jsonDNS2.get("Authentication Token").asText());
            if (jsonWidget != null)
                widgetConfig = new WidgetConfig(jsonWidget.get("Size").asText(), jsonWidget.get("Layout").asText(), true, true, true, 5, 5, 5);

            /*
            if(configDNS1.getIPAddress().isEmpty() && configDNS2.getIPAddress().isEmpty())
            {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Please add an IP address to your configuration", ButtonType.OK);
                alert.setHeaderText("No IP Found");
                alert.showAndWait();
            }
            */

        } catch (Exception e) {
            System.out.println("readConfiguration error: "+e.getMessage());
        }

    }

    public boolean saveEmptyConfiguration() {

        SETTTINGS_FILE = HelperService.createFile(home, file_name, folder_name);

        return writeConfigFile("http", "pi.hole", 80, "", "http", "",80,"", "Medium", "Square", true, true, true,5,5,5);

    }

    public boolean writeConfigFile(String scheme1, String ip1, int port1, String auth1, String scheme2, String ip2, int port2, String auth2, String size, String layout, boolean show_live, boolean show_status, boolean show_fluid
            , int update_status_sec, int update_fluid_sec, int update_active_sec) {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonObject = mapper.createObjectNode();

        ObjectNode jsonDNS1 = mapper.createObjectNode();
        jsonDNS1.put("Scheme", scheme1);
        jsonDNS1.put("IP", ip1);
        jsonDNS1.put("Port", port1);
        jsonDNS1.put("Authentication Token", auth1);

        ObjectNode jsonDNS2 = mapper.createObjectNode();
        jsonDNS2.put("Scheme", scheme2);
        jsonDNS2.put("IP", ip2);
        jsonDNS2.put("Port", port2);
        jsonDNS2.put("Authentication Token", auth2);

        ObjectNode jsonWidget = mapper.createObjectNode();
        jsonWidget.put("Size", size);
        jsonWidget.put("Layout", layout);
        //jsonWidget.put("show_live", show_live);
        //jsonWidget.put("show_status", show_status);
        //jsonWidget.put("show_fluid", show_fluid);

        jsonObject.set("DNS1", jsonDNS1);
        jsonObject.set("DNS2", jsonDNS2);
        jsonObject.set("Widget", jsonWidget);

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(file_path), jsonObject);
            System.out.println("JSON Written.");
            return true;
        } catch (IOException e) {
            System.out.println("Couldn't write JSON.");
            e.printStackTrace();
        }

        return false;
    }

    public PiholeConfig getConfigDNS1() {
        return configDNS1;
    }

    public PiholeConfig getConfigDNS2() {
        return configDNS2;
    }

    public WidgetConfig getWidgetConfig() {
        return widgetConfig;
    }
}
