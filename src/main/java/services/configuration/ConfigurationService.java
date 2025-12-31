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

package services.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import domain.configuration.DnsBlockerConfig;
import domain.configuration.DnsBlockerType;
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
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("dnsbloquer.verbose", "false"));

    // Configuration file location
    private static final String FOLDER_NAME = "DNSBlocker Widget";
    private static final String FILE_NAME = "settings.json";
    private static final String HOME = System.getProperty("user.home");

    // Default values are now consolidated in domain configuration classes

    // JSON keys
    private static final String KEY_DNS1 = "DNS1";
    // DNS2 support intentionally disabled.
    // User request: "no need for 2 DNS management, comment all code related to 2
    // DNSs"
    // private static final String KEY_DNS2 = "DNS2";
    private static final String KEY_WIDGET = "Widget";
    private static final String KEY_PLATFORM = "Platform";
    private static final String KEY_SCHEME = "Scheme";
    private static final String KEY_IP = "IP";
    private static final String KEY_PORT = "Port";
    private static final String KEY_USERNAME = "Username";
    private static final String KEY_AUTH = "Authentication Token";
    private static final String KEY_SIZE = "Size";
    private static final String KEY_LAYOUT = "Layout";
    private static final String KEY_THEME = "Theme";
    private static final String KEY_UPDATE_STATUS = "UpdateStatusSec";
    private static final String KEY_UPDATE_FLUID = "UpdateFluidSec";
    private static final String KEY_UPDATE_ACTIVE = "UpdateActiveSec";
    private static final String KEY_UPDATE_TOPX = "UpdateTopXSec";

    private final Path configFilePath;
    private final ObjectMapper objectMapper;

    private DnsBlockerConfig configDNS1;
    // DNS2 support intentionally disabled.
    // private DnsBlockerConfig configDNS2;
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

    private DnsBlockerConfig parseDnsConfig(JsonNode node) {
        if (node == null) {
            return null;
        }

        // Read platform type (defaults to PIHOLE for backward compatibility)
        String platformStr = getTextOrDefault(node, KEY_PLATFORM, "");
        DnsBlockerType platform = DnsBlockerType.fromString(platformStr);

        String ip = getTextOrDefault(node, KEY_IP, "");
        int port = getIntOrDefault(node, KEY_PORT, DnsBlockerConfig.DEFAULT_PORT);
        String scheme = getTextOrDefault(node, KEY_SCHEME, DnsBlockerConfig.DEFAULT_SCHEME);
        String username = getTextOrDefault(node, KEY_USERNAME, DnsBlockerConfig.DEFAULT_USERNAME);
        String auth = getTextOrDefault(node, KEY_AUTH, "");

        return new DnsBlockerConfig(platform, ip, port, scheme, username, auth);
    }

    private WidgetConfig parseWidgetConfig(JsonNode node) {
        if (node == null) {
            return WidgetConfig.defaultConfig();
        }

        String size = getTextOrDefault(node, KEY_SIZE, WidgetConfig.DEFAULT_SIZE);
        String layout = getTextOrDefault(node, KEY_LAYOUT, WidgetConfig.DEFAULT_LAYOUT);
        String theme = getTextOrDefault(node, KEY_THEME, WidgetConfig.DEFAULT_THEME);
        int updateStatus = getIntOrDefault(node, KEY_UPDATE_STATUS, WidgetConfig.DEFAULT_STATUS_UPDATE_SEC);
        int updateFluid = getIntOrDefault(node, KEY_UPDATE_FLUID, WidgetConfig.DEFAULT_FLUID_UPDATE_SEC);
        int updateActive = getIntOrDefault(node, KEY_UPDATE_ACTIVE, WidgetConfig.DEFAULT_ACTIVE_UPDATE_SEC);
        int updateTopX = getIntOrDefault(node, KEY_UPDATE_TOPX, WidgetConfig.DEFAULT_TOPX_UPDATE_SEC);

        return new WidgetConfig(size, layout, theme, true, true, true, updateStatus, updateFluid, updateActive,
                updateTopX);
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
                DnsBlockerType.PIHOLE, DnsBlockerConfig.DEFAULT_SCHEME, DnsBlockerConfig.DEFAULT_IP,
                DnsBlockerConfig.DEFAULT_PORT, DnsBlockerConfig.DEFAULT_USERNAME, "",
                DnsBlockerType.PIHOLE, DnsBlockerConfig.DEFAULT_SCHEME, "",
                DnsBlockerConfig.DEFAULT_PORT, DnsBlockerConfig.DEFAULT_USERNAME, "",
                WidgetConfig.DEFAULT_SIZE, WidgetConfig.DEFAULT_LAYOUT, WidgetConfig.DEFAULT_THEME,
                true, true, true,
                WidgetConfig.DEFAULT_STATUS_UPDATE_SEC,
                WidgetConfig.DEFAULT_FLUID_UPDATE_SEC,
                WidgetConfig.DEFAULT_ACTIVE_UPDATE_SEC,
                WidgetConfig.DEFAULT_TOPX_UPDATE_SEC);
    }

    /**
     * Writes configuration to the settings file.
     */
    public boolean writeConfigFile(
            DnsBlockerType platform1, String scheme1, String ip1, int port1, String username1, String auth1,
            DnsBlockerType platform2, String scheme2, String ip2, int port2, String username2, String auth2,
            String size, String layout, String theme,
            boolean showLive, boolean showStatus, boolean showFluid,
            int updateStatusSec, int updateFluidSec, int updateActiveSec, int updateTopXSec) {

        log("Writing configuration to: " + configFilePath);

        ObjectNode root = objectMapper.createObjectNode();

        // DNS1 configuration
        ObjectNode dns1Node = objectMapper.createObjectNode();
        dns1Node.put(KEY_PLATFORM, platform1 != null ? platform1.name() : DnsBlockerType.PIHOLE.name());
        dns1Node.put(KEY_SCHEME, scheme1);
        dns1Node.put(KEY_IP, ip1);
        dns1Node.put(KEY_PORT, port1);
        dns1Node.put(KEY_USERNAME, username1 != null ? username1 : "");
        dns1Node.put(KEY_AUTH, auth1);
        root.set(KEY_DNS1, dns1Node);

        /*
         * DNS2 support intentionally disabled.
         * User request:
         * "no need for 2 DNS management, comment all code related to 2 DNSs"
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
        widgetNode.put(KEY_THEME, theme != null ? theme : WidgetConfig.DEFAULT_THEME);
        widgetNode.put(KEY_UPDATE_STATUS, updateStatusSec);
        widgetNode.put(KEY_UPDATE_FLUID, updateFluidSec);
        widgetNode.put(KEY_UPDATE_ACTIVE, updateActiveSec);
        widgetNode.put(KEY_UPDATE_TOPX, updateTopXSec);
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

    public DnsBlockerConfig getConfigDNS1() {
        return configDNS1;
    }

    public DnsBlockerConfig getConfigDNS2() {
        // DNS2 support intentionally disabled.
        // return configDNS2;
        return null;
    }

    public WidgetConfig getWidgetConfig() {
        return widgetConfig;
    }
}
