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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import domain.configuration.PiholeConfig;
import domain.configuration.WidgetConfig;
import helpers.HelperService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for reading and writing application configuration.
 * Configuration is stored as JSON in the user's home directory.
 */
public class ConfigurationService {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationService.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));
    
    // Configuration file location
    private static final String FOLDER_NAME = "Pihole Widget";
    private static final String FILE_NAME = "settings.json";
    private static final String HOME = System.getProperty("user.home");
    
    // Default values
    private static final String DEFAULT_SCHEME = "http";
    private static final String DEFAULT_IP = "pi.hole";
    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_SIZE = "Medium";
    private static final String DEFAULT_LAYOUT = "Square";
    
    // JSON keys
    private static final String KEY_DNS1 = "DNS1";
    // DNS2 support intentionally disabled.
    // User request: "no need for 2 DNS management, comment all code related to 2 DNSs"
    // private static final String KEY_DNS2 = "DNS2";
    private static final String KEY_WIDGET = "Widget";
    private static final String KEY_SCHEME = "Scheme";
    private static final String KEY_IP = "IP";
    private static final String KEY_PORT = "Port";
    private static final String KEY_AUTH = "Authentication Token";
    private static final String KEY_SIZE = "Size";
    private static final String KEY_LAYOUT = "Layout";
    
    private final Path configFilePath;
    private final ObjectMapper objectMapper;
    
    private PiholeConfig configDNS1;
    // DNS2 support intentionally disabled.
    // private PiholeConfig configDNS2;
    private WidgetConfig widgetConfig;

    public ConfigurationService() {
        this.configFilePath = Path.of(HOME, FOLDER_NAME, FILE_NAME);
        this.objectMapper = new ObjectMapper();
    }
    
    private static void log(String message) {
        if (VERBOSE) {
            LOGGER.log(Level.FINE, () -> "[ConfigService] " + message);
        }
    }

    /**
     * Reads the configuration from the settings file.
     * If the file doesn't exist, creates it with default values.
     */
    public void readConfiguration() {
        log("Reading configuration from: " + configFilePath);
        
        // Create config file if it doesn't exist
        if (!Files.exists(configFilePath)) {
            log("Configuration file not found, creating default");
            saveEmptyConfiguration();
        }

        try {
            JsonNode root = objectMapper.readTree(configFilePath.toFile());
            
            configDNS1 = parseDnsConfig(root.get(KEY_DNS1));
            // DNS2 support intentionally disabled.
            // configDNS2 = parseDnsConfig(root.get(KEY_DNS2));
            widgetConfig = parseWidgetConfig(root.get(KEY_WIDGET));
            
            log("Configuration loaded successfully");
            log("DNS1: " + (configDNS1 != null ? configDNS1.getIPAddress() : "null"));
            // DNS2 support intentionally disabled.
            // log("DNS2: " + (configDNS2 != null ? configDNS2.getIPAddress() : "null"));
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read configuration", e);
        }
    }
    
    private PiholeConfig parseDnsConfig(JsonNode node) {
        if (node == null) {
            return null;
        }
        
        String ip = getTextOrDefault(node, KEY_IP, "");
        int port = getIntOrDefault(node, KEY_PORT, DEFAULT_PORT);
        String scheme = getTextOrDefault(node, KEY_SCHEME, DEFAULT_SCHEME);
        String auth = getTextOrDefault(node, KEY_AUTH, "");
        
        return new PiholeConfig(ip, port, scheme, auth);
    }
    
    private WidgetConfig parseWidgetConfig(JsonNode node) {
        if (node == null) {
            return WidgetConfig.defaultConfig();
        }
        
        String size = getTextOrDefault(node, KEY_SIZE, DEFAULT_SIZE);
        String layout = getTextOrDefault(node, KEY_LAYOUT, DEFAULT_LAYOUT);
        
        return new WidgetConfig(size, layout);
    }
    
    private String getTextOrDefault(JsonNode node, String key, String defaultValue) {
        if (node == null || !node.has(key) || node.get(key).isNull()) {
            return defaultValue;
        }
        return node.get(key).asText(defaultValue);
    }
    
    private int getIntOrDefault(JsonNode node, String key, int defaultValue) {
        if (node == null || !node.has(key) || node.get(key).isNull()) {
            return defaultValue;
        }
        return node.get(key).asInt(defaultValue);
    }

    /**
     * Creates the configuration file with default values.
     */
    public boolean saveEmptyConfiguration() {
        log("Creating empty configuration file");
        
        // Ensure parent directory exists
        HelperService.createFile(HOME, FILE_NAME, FOLDER_NAME);
        
        return writeConfigFile(
                DEFAULT_SCHEME, DEFAULT_IP, DEFAULT_PORT, "",
                DEFAULT_SCHEME, "", DEFAULT_PORT, "",
                DEFAULT_SIZE, DEFAULT_LAYOUT,
                true, true, true, 5, 5, 5
        );
    }

    /**
     * Writes configuration to the settings file.
     */
    public boolean writeConfigFile(
            String scheme1, String ip1, int port1, String auth1,
            String scheme2, String ip2, int port2, String auth2,
            String size, String layout,
            boolean showLive, boolean showStatus, boolean showFluid,
            int updateStatusSec, int updateFluidSec, int updateActiveSec) {
        
        log("Writing configuration to: " + configFilePath);

        ObjectNode root = objectMapper.createObjectNode();

        // DNS1 configuration
        ObjectNode dns1Node = objectMapper.createObjectNode();
        dns1Node.put(KEY_SCHEME, scheme1);
        dns1Node.put(KEY_IP, ip1);
        dns1Node.put(KEY_PORT, port1);
        dns1Node.put(KEY_AUTH, auth1);
        root.set(KEY_DNS1, dns1Node);

        /*
         * DNS2 support intentionally disabled.
         * User request: "no need for 2 DNS management, comment all code related to 2 DNSs"
         *
         * // DNS2 configuration
         * ObjectNode dns2Node = objectMapper.createObjectNode();
         * dns2Node.put(KEY_SCHEME, scheme2);
         * dns2Node.put(KEY_IP, ip2);
         * dns2Node.put(KEY_PORT, port2);
         * dns2Node.put(KEY_AUTH, auth2);
         * root.set(KEY_DNS2, dns2Node);
         */

        // Widget configuration
        ObjectNode widgetNode = objectMapper.createObjectNode();
        widgetNode.put(KEY_SIZE, size);
        widgetNode.put(KEY_LAYOUT, layout);
        root.set(KEY_WIDGET, widgetNode);

        try {
            // Ensure parent directory exists
            Files.createDirectories(configFilePath.getParent());
            
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(configFilePath.toFile(), root);
            
            log("Configuration written successfully");
            return true;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write configuration", e);
            return false;
        }
    }

    // ==================== Getters ====================

    public PiholeConfig getConfigDNS1() {
        return configDNS1;
    }

    public PiholeConfig getConfigDNS2() {
        // DNS2 support intentionally disabled.
        // return configDNS2;
        return null;
    }

    public WidgetConfig getWidgetConfig() {
        return widgetConfig;
    }
}
