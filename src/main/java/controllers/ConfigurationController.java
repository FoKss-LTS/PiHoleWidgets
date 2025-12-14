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

package controllers;

import domain.configuration.PiholeConfig;
import domain.configuration.WidgetConfig;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import services.configuration.ConfigurationService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the configuration window.
 * Handles Pi-hole DNS server configuration and widget display settings.
 */
public class ConfigurationController implements Initializable {

    // ==================== Constants ====================
    
    private static final Logger LOGGER = Logger.getLogger(ConfigurationController.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));
    
    private static final List<String> SIZES = List.of("Small", "Medium", "Large", "XXL", "Full Screen");
    private static final List<String> LAYOUTS = List.of("Horizontal", "Square");
    private static final List<String> SCHEMES = List.of("http", "https");
    private static final List<String> THEMES = List.of("Dark", "Light");
    
    private static final String DEFAULT_SCHEME = "http";
    private static final String DEFAULT_SIZE = "Medium";
    private static final String DEFAULT_LAYOUT = "Square";
    private static final String DEFAULT_THEME = "Dark";
    private static final int DEFAULT_PORT = 80;
    
    
    // Button styles
    private static final String APPLY_BTN_NORMAL = "-fx-background-color: #4a9eff; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;";
    private static final String APPLY_BTN_HOVER = "-fx-background-color: #5aaeff; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;";
    private static final String LOAD_BTN_NORMAL = "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;";
    private static final String LOAD_BTN_HOVER = "-fx-background-color: #7c858d; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;";
    private static final String SAVE_BTN_NORMAL = "-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;";
    private static final String SAVE_BTN_HOVER = "-fx-background-color: #38b755; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;";
    private static final String CANCEL_BTN_NORMAL = "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;";
    private static final String CANCEL_BTN_HOVER = "-fx-background-color: #ec4555; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;";

    // ==================== FXML Injected Fields ====================
    
    @FXML private Button buttonCancel;
    @FXML private Button buttonSave;
    @FXML private Button buttonLoad;
    @FXML private Button buttonApply;
    
    @FXML private TitledPane dns1TitledPane;
    @FXML private Accordion accord;
    
    @FXML private TextField tfIp1;
    @FXML private TextField tfPort1;
    @FXML private TextField tfAuth1;
    // DNS2 support intentionally disabled.
    // @FXML private TextField tfIp2;
    // @FXML private TextField tfPort2;
    // @FXML private TextField tfAuth2;
    
    @FXML private ComboBox<String> comboBoxSize;
    @FXML private ComboBox<String> comboBoxLayout;
    @FXML private ComboBox<String> comboBoxTheme;
    @FXML private ComboBox<String> comboBoxScheme1;
    // DNS2 support intentionally disabled.
    // @FXML private ComboBox<String> comboBoxScheme2;

    // Legacy FXML field names for backward compatibility with existing FXML
    @FXML private Button button_cancel;
    @FXML private Button button_save;
    @FXML private Button button_load;
    @FXML private Button button_apply;
    @FXML private TextField TF_IP1;
    @FXML private TextField TF_Port1;
    @FXML private TextField TF_AUTH1;
    // DNS2 support intentionally disabled.
    // @FXML private TextField TF_IP2;
    // @FXML private TextField TF_Port2;
    // @FXML private TextField TG_AUTH2;
    @FXML private ComboBox<String> ComboBoxSize;
    @FXML private ComboBox<String> ComboBoxLayout;
    @FXML private ComboBox<String> ComboBoxTheme;
    @FXML private ComboBox<String> ComboBox_Scheme1;
    // DNS2 support intentionally disabled.
    // @FXML private ComboBox<String> ComboBox_Scheme2;

    // ==================== Instance Fields ====================
    
    private PiholeConfig configDNS1;
    // DNS2 support intentionally disabled.
    // private PiholeConfig configDNS2;
    private WidgetConfig widgetConfig;
    
    // ==================== Constructor ====================

    public ConfigurationController(PiholeConfig configDNS1, WidgetConfig widgetConfig) {
        log("ConfigurationController created");
        this.configDNS1 = configDNS1;
        this.widgetConfig = widgetConfig;
    }
    
    // ==================== Logging ====================
    
    private static void log(String message) {
        if (VERBOSE) {
            LOGGER.log(Level.FINE, () -> "[Config] " + message);
        }
    }

    // ==================== Initialization ====================
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log("Initializing ConfigurationController");
        
        // Resolve legacy field names
        resolveLegacyFields();
        
        // Initialize ComboBoxes
        initializeComboBoxes();
        
        // Setup button actions
        setupButtonActions();
        
        // Setup button hover effects
        setupButtonHoverEffects();
        
        // Expand DNS1 pane by default
        if (accord != null && dns1TitledPane != null) {
            accord.setExpandedPane(dns1TitledPane);
        }
        
        // Load configuration
        loadConfiguration();
        
        log("ConfigurationController initialization complete");
    }
    
    private void resolveLegacyFields() {
        // Map legacy FXML field names to new names
        if (buttonCancel == null) buttonCancel = button_cancel;
        if (buttonSave == null) buttonSave = button_save;
        if (buttonLoad == null) buttonLoad = button_load;
        if (buttonApply == null) buttonApply = button_apply;
        if (tfIp1 == null) tfIp1 = TF_IP1;
        if (tfPort1 == null) tfPort1 = TF_Port1;
        if (tfAuth1 == null) tfAuth1 = TF_AUTH1;
        if (comboBoxSize == null) comboBoxSize = ComboBoxSize;
        if (comboBoxLayout == null) comboBoxLayout = ComboBoxLayout;
        if (comboBoxTheme == null) comboBoxTheme = ComboBoxTheme;
        if (comboBoxScheme1 == null) comboBoxScheme1 = ComboBox_Scheme1;
        // DNS2 support intentionally disabled.
        // if (tfIp2 == null) tfIp2 = TF_IP2;
        // if (tfPort2 == null) tfPort2 = TF_Port2;
        // if (tfAuth2 == null) tfAuth2 = TG_AUTH2;
        // if (comboBoxScheme2 == null) comboBoxScheme2 = ComboBox_Scheme2;
    }
    
    private void initializeComboBoxes() {
        // Size options
        if (comboBoxSize != null) {
            comboBoxSize.setItems(FXCollections.observableArrayList(SIZES));
        }
        
        // Layout options
        if (comboBoxLayout != null) {
            comboBoxLayout.setItems(FXCollections.observableArrayList(LAYOUTS));
        }
        
        // Theme options
        if (comboBoxTheme != null) {
            comboBoxTheme.setItems(FXCollections.observableArrayList(THEMES));
            comboBoxTheme.setValue(DEFAULT_THEME);
        }
        
        // Scheme options for DNS1
        if (comboBoxScheme1 != null) {
            comboBoxScheme1.setItems(FXCollections.observableArrayList(SCHEMES));
            comboBoxScheme1.setValue(DEFAULT_SCHEME);
        }

        /*
         * DNS2 support intentionally disabled.
         * User request: "no need for 2 DNS management, comment all code related to 2 DNSs"
         *
         * // Scheme options for DNS2
         * if (comboBoxScheme2 != null) {
         *     comboBoxScheme2.setItems(FXCollections.observableArrayList(SCHEMES));
         *     comboBoxScheme2.setValue(DEFAULT_SCHEME);
         *     applyDarkTheme(comboBoxScheme2);
         * }
         */
    }
    
    private void setupButtonActions() {
        if (buttonApply != null) {
            buttonApply.setOnMouseClicked(_ -> {
                log("Apply button clicked");
                saveConfiguration();
                WidgetApplication.applyAndCloseConfigurationWindow();
            });
        }
        
        if (buttonSave != null) {
            buttonSave.setOnMouseClicked(_ -> {
                log("Save button clicked");
                saveConfiguration();
            });
        }
        
        if (buttonLoad != null) {
            buttonLoad.setOnMouseClicked(_ -> {
                log("Load button clicked");
                loadConfiguration();
            });
        }
        
        if (buttonCancel != null) {
            buttonCancel.setOnMouseClicked(_ -> {
                log("Cancel button clicked");
                WidgetApplication.closeConfigurationWindow();
            });
        }
    }

    private void setupButtonHoverEffects() {
        setupHoverEffect(buttonApply, APPLY_BTN_NORMAL, APPLY_BTN_HOVER);
        setupHoverEffect(buttonLoad, LOAD_BTN_NORMAL, LOAD_BTN_HOVER);
        setupHoverEffect(buttonSave, SAVE_BTN_NORMAL, SAVE_BTN_HOVER);
        setupHoverEffect(buttonCancel, CANCEL_BTN_NORMAL, CANCEL_BTN_HOVER);
    }
    
    private void setupHoverEffect(Button button, String normalStyle, String hoverStyle) {
        if (button != null) {
            button.setOnMouseEntered(_ -> button.setStyle(hoverStyle));
            button.setOnMouseExited(_ -> button.setStyle(normalStyle));
        }
    }


    // ==================== Configuration Management ====================

    @FXML
    public void saveConfiguration() {
        log("Saving configuration...");
        
        ConfigurationService configService = new ConfigurationService();
        
        // Parse ports with default fallback
        int port1 = parsePort(tfPort1);
        // DNS2 support intentionally disabled.
        // int port2 = parsePort(tfPort2);
        
        // Get schemes
        String scheme1 = getSelectedOrDefault(comboBoxScheme1, DEFAULT_SCHEME);
        // DNS2 support intentionally disabled.
        // String scheme2 = getSelectedOrDefault(comboBoxScheme2, DEFAULT_SCHEME);
        
        // Strip any scheme prefix from IP fields (backward compatibility)
        String ip1 = stripScheme(getTextOrEmpty(tfIp1));
        // DNS2 support intentionally disabled.
        // String ip2 = stripScheme(getTextOrEmpty(tfIp2));
        
        // Get widget settings
        String size = getSelectedOrDefault(comboBoxSize, DEFAULT_SIZE);
        String layout = getSelectedOrDefault(comboBoxLayout, DEFAULT_LAYOUT);
        String theme = getSelectedOrDefault(comboBoxTheme, DEFAULT_THEME);
        
        log("Saving - DNS1: " + scheme1 + "://" + ip1 + ":" + port1);
        // DNS2 support intentionally disabled.
        // log("Saving - DNS2: " + scheme2 + "://" + ip2 + ":" + port2);
        log("Saving - Widget: size=" + size + ", layout=" + layout + ", theme=" + theme);
        
        configService.writeConfigFile(
                scheme1, ip1, port1, getTextOrEmpty(tfAuth1),
                // DNS2 support intentionally disabled - parameters kept for backward compatible signature.
                DEFAULT_SCHEME, "", DEFAULT_PORT, "",
                size, layout, theme, true, true, true, 5, 5, 5
        );
        
        log("Configuration saved");
    }

    @FXML
    public void loadConfiguration() {
        log("Loading configuration...");
        
        ConfigurationService configService = new ConfigurationService();
        configService.readConfiguration();
        
        configDNS1 = configService.getConfigDNS1();
        widgetConfig = configService.getWidgetConfig();
        
        // Populate DNS1 fields
        if (configDNS1 != null) {
            log("Loading DNS1: " + configDNS1.getIPAddress());
            setComboBoxValue(comboBoxScheme1, configDNS1.getScheme(), DEFAULT_SCHEME);
            setTextFieldValue(tfIp1, configDNS1.getIPAddress());
            setTextFieldValue(tfPort1, String.valueOf(configDNS1.getPort()));
            setTextFieldValue(tfAuth1, configDNS1.getAUTH());
        } else {
            log("DNS1 config is null, using defaults");
            setComboBoxValue(comboBoxScheme1, DEFAULT_SCHEME, DEFAULT_SCHEME);
            setTextFieldValue(tfIp1, "");
        }
        
        /*
         * DNS2 support intentionally disabled.
         * User request: "no need for 2 DNS management, comment all code related to 2 DNSs"
         *
         * // Populate DNS2 fields
         * if (configDNS2 != null) {
         *     log("Loading DNS2: " + configDNS2.getIPAddress());
         *     setComboBoxValue(comboBoxScheme2, configDNS2.getScheme(), DEFAULT_SCHEME);
         *     setTextFieldValue(tfIp2, configDNS2.getIPAddress());
         *     setTextFieldValue(tfPort2, String.valueOf(configDNS2.getPort()));
         *     setTextFieldValue(tfAuth2, configDNS2.getAUTH());
         * } else {
         *     log("DNS2 config is null, using defaults");
         *     setComboBoxValue(comboBoxScheme2, DEFAULT_SCHEME, DEFAULT_SCHEME);
         *     setTextFieldValue(tfIp2, "");
         * }
         */
        
        // Populate widget settings
        if (widgetConfig != null) {
            log("Loading widget config: size=" + widgetConfig.getSize() + ", layout=" + widgetConfig.getLayout() + ", theme=" + widgetConfig.getTheme());
            setComboBoxValue(comboBoxSize, widgetConfig.getSize(), DEFAULT_SIZE);
            setComboBoxValue(comboBoxLayout, widgetConfig.getLayout(), DEFAULT_LAYOUT);
            setComboBoxValue(comboBoxTheme, widgetConfig.getTheme(), DEFAULT_THEME);
        }
        
        log("Configuration loaded");
    }

    // ==================== Utility Methods ====================
    
    private int parsePort(TextField field) {
        if (field == null) return DEFAULT_PORT;
        String text = field.getText();
        if (text == null || text.isBlank()) return DEFAULT_PORT;
        try {
            int port = Integer.parseInt(text.trim());
            return (port > 0 && port <= 65535) ? port : DEFAULT_PORT;
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }
    
    private String getTextOrEmpty(TextField field) {
        return (field != null && field.getText() != null) ? field.getText().trim() : "";
    }
    
    private String getSelectedOrDefault(ComboBox<String> comboBox, String defaultValue) {
        if (comboBox == null || comboBox.getValue() == null) return defaultValue;
        return comboBox.getValue();
    }
    
    private void setComboBoxValue(ComboBox<String> comboBox, String value, String defaultValue) {
        if (comboBox != null) {
            comboBox.setValue(value != null && !value.isBlank() ? value : defaultValue);
        }
    }
    
    private void setTextFieldValue(TextField field, String value) {
        if (field != null) {
            field.setText(value != null ? value : "");
        }
    }
    
    /**
     * Removes scheme prefix and trailing slash from a host string.
     */
    private String stripScheme(String rawHost) {
        if (rawHost == null) return "";
        String host = rawHost.trim();
        
        if (host.startsWith("https://")) {
            host = host.substring("https://".length());
        } else if (host.startsWith("http://")) {
            host = host.substring("http://".length());
        }
        
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        
        return host;
    }
}
