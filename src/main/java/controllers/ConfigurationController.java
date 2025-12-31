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

package controllers;

import domain.configuration.DnsBlockerConfig;
import domain.configuration.DnsBlockerType;
import domain.configuration.WidgetConfig;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.Window;
import services.configuration.ConfigurationService;

import java.net.URI;
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
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("dnsbloquer.verbose", "false"));

    private static final List<String> SIZES = List.of("Small", "Medium", "Large", "XXL", "Full Screen");
    private static final List<String> LAYOUTS = List.of("Horizontal", "Square");
    private static final List<String> SCHEMES = List.of("http", "https");
    private static final List<String> THEMES = List.of("Dark", "Light");

    // Default values are now consolidated in domain configuration classes
    private static final String DEFAULT_SCHEME = DnsBlockerConfig.DEFAULT_SCHEME;
    private static final String DEFAULT_SIZE = WidgetConfig.DEFAULT_SIZE;
    private static final String DEFAULT_LAYOUT = WidgetConfig.DEFAULT_LAYOUT;
    private static final String DEFAULT_THEME = WidgetConfig.DEFAULT_THEME;
    private static final int DEFAULT_PORT = DnsBlockerConfig.DEFAULT_PORT;
    private static final int DEFAULT_UPDATE_STATUS_SEC = WidgetConfig.DEFAULT_STATUS_UPDATE_SEC;
    private static final int DEFAULT_UPDATE_FLUID_SEC = WidgetConfig.DEFAULT_FLUID_UPDATE_SEC;
    private static final int DEFAULT_UPDATE_ACTIVE_SEC = WidgetConfig.DEFAULT_ACTIVE_UPDATE_SEC;
    private static final int DEFAULT_UPDATE_TOPX_SEC = WidgetConfig.DEFAULT_TOPX_UPDATE_SEC;
    private static final int DEFAULT_TOPX_COUNT = WidgetConfig.DEFAULT_TOPX_COUNT;

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

    @FXML
    private Button buttonCancel;
    @FXML
    private Button buttonSave;
    @FXML
    private Button buttonLoad;
    @FXML
    private Button buttonApply;

    @FXML
    private TitledPane dns1TitledPane;
    @FXML
    private Accordion accord;

    @FXML
    private TextField tfIp1;
    @FXML
    private TextField tfPort1;
    @FXML
    private TextField tfAuth1;
    @FXML
    private TextField tfUsername1;
    @FXML
    private TextField tfUpdateStatus;
    @FXML
    private TextField tfUpdateFluid;
    @FXML
    private TextField tfUpdateActive;
    @FXML
    private TextField tfUpdateTopX;
    @FXML
    private TextField tfTopXCount;
    // DNS2 support intentionally disabled.
    // @FXML private TextField tfIp2;
    // @FXML private TextField tfPort2;
    // @FXML private TextField tfAuth2;

    @FXML
    private ComboBox<String> comboBoxSize;
    @FXML
    private ComboBox<String> comboBoxLayout;
    @FXML
    private ComboBox<String> comboBoxTheme;
    @FXML
    private ComboBox<String> comboBoxScheme1;
    @FXML
    private ComboBox<String> comboBoxPlatform1;
    // DNS2 support intentionally disabled.
    // @FXML private ComboBox<String> comboBoxScheme2;

    // Legacy FXML field names for backward compatibility with existing FXML
    @FXML
    private Button button_cancel;
    @FXML
    private Button button_save;
    @FXML
    private Button button_load;
    @FXML
    private Button button_apply;
    @FXML
    private TextField TF_IP1;
    @FXML
    private TextField TF_Port1;
    @FXML
    private TextField TF_AUTH1;
    @FXML
    private TextField TF_Username1;
    @FXML
    private ComboBox<String> ComboBoxSize;
    @FXML
    private ComboBox<String> ComboBoxLayout;
    @FXML
    private ComboBox<String> ComboBoxTheme;
    @FXML
    private ComboBox<String> ComboBox_Scheme1;
    @FXML
    private ComboBox<String> ComboBox_Platform1;
    @FXML
    private Label Label_Username1;
    @FXML
    private Label Label_Password1;
    // DNS2 support intentionally disabled.
    // @FXML private ComboBox<String> ComboBox_Scheme2;

    // ==================== Instance Fields ====================

    private DnsBlockerConfig configDNS1;
    // DNS2 support intentionally disabled.
    // private DnsBlockerConfig configDNS2;
    private WidgetConfig widgetConfig;

    // ==================== Constructor ====================

    public ConfigurationController(DnsBlockerConfig configDNS1, WidgetConfig widgetConfig) {
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
        if (buttonCancel == null)
            buttonCancel = button_cancel;
        if (buttonSave == null)
            buttonSave = button_save;
        if (buttonLoad == null)
            buttonLoad = button_load;
        if (buttonApply == null)
            buttonApply = button_apply;
        if (tfIp1 == null)
            tfIp1 = TF_IP1;
        if (tfPort1 == null)
            tfPort1 = TF_Port1;
        if (tfAuth1 == null)
            tfAuth1 = TF_AUTH1;
        if (comboBoxSize == null)
            comboBoxSize = ComboBoxSize;
        if (comboBoxLayout == null)
            comboBoxLayout = ComboBoxLayout;
        if (comboBoxTheme == null)
            comboBoxTheme = ComboBoxTheme;
        if (comboBoxScheme1 == null)
            comboBoxScheme1 = ComboBox_Scheme1;
        if (comboBoxPlatform1 == null)
            comboBoxPlatform1 = ComboBox_Platform1;
        if (tfUsername1 == null)
            tfUsername1 = TF_Username1;
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

        // Platform options for DNS1
        if (comboBoxPlatform1 != null) {
            comboBoxPlatform1.setItems(FXCollections.observableArrayList(
                    DnsBlockerType.PIHOLE.getDisplayName(),
                    DnsBlockerType.ADGUARD_HOME.getDisplayName()));
            comboBoxPlatform1.setValue(DnsBlockerType.PIHOLE.getDisplayName()); // Default to Pi-hole

            // Add listener to show/hide username field based on platform
            comboBoxPlatform1.setOnAction(_ -> updateUsernameFieldVisibility());
        }

        /*
         * DNS2 support intentionally disabled.
         * User request:
         * "no need for 2 DNS management, comment all code related to 2 DNSs"
         *
         * // Scheme options for DNS2
         * if (comboBoxScheme2 != null) {
         * comboBoxScheme2.setItems(FXCollections.observableArrayList(SCHEMES));
         * comboBoxScheme2.setValue(DEFAULT_SCHEME);
         * applyDarkTheme(comboBoxScheme2);
         * }
         */
    }

    private void setupButtonActions() {
        if (buttonApply != null) {
            buttonApply.setOnMouseClicked(_ -> {
                log("Apply button clicked");
                saveConfiguration();
                showInfoAlert("Settings applied", "Configuration saved and applied to the widget.");
                WidgetApplication.applyAndCloseConfigurationWindow();
            });
        }

        if (buttonSave != null) {
            buttonSave.setOnMouseClicked(_ -> {
                log("Save button clicked");
                saveConfiguration();
                showInfoAlert("Configuration saved", "Settings saved to the configuration file.");
            });
        }

        if (buttonLoad != null) {
            buttonLoad.setOnMouseClicked(_ -> {
                log("Load button clicked");
                loadConfiguration();
                showInfoAlert("Configuration loaded", "Settings loaded from the configuration file.");
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

    /**
     * Updates the visibility of the username field based on the selected platform.
     * AdGuard Home requires a username for Basic Auth, Pi-hole does not.
     */
    private void updateUsernameFieldVisibility() {
        if (comboBoxPlatform1 == null)
            return;

        String selectedPlatform = comboBoxPlatform1.getValue();
        boolean isAdGuardHome = DnsBlockerType.ADGUARD_HOME.getDisplayName().equals(selectedPlatform);

        // Show/hide username field and label
        if (tfUsername1 != null) {
            tfUsername1.setVisible(isAdGuardHome);
            tfUsername1.setManaged(isAdGuardHome);
        }
        if (Label_Username1 != null) {
            Label_Username1.setVisible(isAdGuardHome);
            Label_Username1.setManaged(isAdGuardHome);
        }

        // Update password label text
        if (Label_Password1 != null) {
            Label_Password1.setText(isAdGuardHome ? "Password:" : "App Password:");
        }

        // Keep prompt text consistent with naming (password for both platforms)
        if (tfAuth1 != null) {
            tfAuth1.setPromptText("Enter password");
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

        // Accept user pasted values like:
        // - pi.hole
        // - 192.168.1.2
        // - pi.hole:8080
        // - http(s)://pi.hole:8080
        // but reject paths/query/fragment (those belong nowhere in "host" input).
        ParsedHostInput parsed = parseHostInput(getTextOrEmpty(tfIp1));
        if (!parsed.valid()) {
            showInfoAlert("Invalid Host", parsed.errorMessage());
            return;
        }

        String ip1 = parsed.host();
        if (parsed.port() != null) {
            port1 = parsed.port();
        }
        if (parsed.scheme() != null) {
            scheme1 = parsed.scheme();
        }
        // DNS2 support intentionally disabled.
        // String ip2 = stripScheme(getTextOrEmpty(tfIp2));

        // Get widget settings
        String size = getSelectedOrDefault(comboBoxSize, DEFAULT_SIZE);
        String layout = getSelectedOrDefault(comboBoxLayout, DEFAULT_LAYOUT);
        String theme = getSelectedOrDefault(comboBoxTheme, DEFAULT_THEME);
        int updateStatusSec = parseInterval(tfUpdateStatus, DEFAULT_UPDATE_STATUS_SEC);
        int updateFluidSec = parseInterval(tfUpdateFluid, DEFAULT_UPDATE_FLUID_SEC);
        int updateActiveSec = parseInterval(tfUpdateActive, DEFAULT_UPDATE_ACTIVE_SEC);
        int updateTopXSec = parseInterval(tfUpdateTopX, DEFAULT_UPDATE_TOPX_SEC);
        int topXCount = parseInterval(tfTopXCount, DEFAULT_TOPX_COUNT);

        log("Saving - DNS1: " + scheme1 + "://" + ip1 + ":" + port1);
        // DNS2 support intentionally disabled.
        // log("Saving - DNS2: " + scheme2 + "://" + ip2 + ":" + port2);
        log("Saving - Widget: size=" + size + ", layout=" + layout + ", theme=" + theme);

        // Parse platform selection
        String platformStr = getSelectedOrDefault(comboBoxPlatform1, DnsBlockerType.PIHOLE.getDisplayName());
        DnsBlockerType platform = DnsBlockerType.fromDisplayName(platformStr);
        String username = getTextOrEmpty(tfUsername1);

        configService.writeConfigFile(
                platform, scheme1, ip1, port1, username, getTextOrEmpty(tfAuth1),
                // DNS2 support intentionally disabled - parameters kept for backward compatible
                // signature.
                DnsBlockerType.PIHOLE, DnsBlockerConfig.DEFAULT_SCHEME, "", DnsBlockerConfig.DEFAULT_PORT, "", "",
                size, layout, theme, true, true, true,
                topXCount,
                updateStatusSec, updateFluidSec, updateActiveSec, updateTopXSec);

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

            // Set platform
            if (comboBoxPlatform1 != null) {
                setComboBoxValue(comboBoxPlatform1, configDNS1.platform().getDisplayName(),
                        DnsBlockerType.PIHOLE.getDisplayName());
                updateUsernameFieldVisibility(); // Update field visibility based on loaded platform
            }

            setComboBoxValue(comboBoxScheme1, configDNS1.getScheme(), DEFAULT_SCHEME);
            setTextFieldValue(tfIp1, configDNS1.getIPAddress());
            setTextFieldValue(tfPort1, String.valueOf(configDNS1.getPort()));
            setTextFieldValue(tfUsername1, configDNS1.username() != null ? configDNS1.username() : "");
            setTextFieldValue(tfAuth1, configDNS1.password());
        } else {
            log("DNS1 config is null, using defaults");
            setComboBoxValue(comboBoxPlatform1, DnsBlockerType.PIHOLE.getDisplayName(),
                    DnsBlockerType.PIHOLE.getDisplayName());
            setComboBoxValue(comboBoxScheme1, DEFAULT_SCHEME, DEFAULT_SCHEME);
            setTextFieldValue(tfIp1, "");
            updateUsernameFieldVisibility(); // Update field visibility for default platform
        }

        /*
         * DNS2 support intentionally disabled.
         * User request:
         * "no need for 2 DNS management, comment all code related to 2 DNSs"
         *
         * // Populate DNS2 fields
         * if (configDNS2 != null) {
         * log("Loading DNS2: " + configDNS2.getIPAddress());
         * setComboBoxValue(comboBoxScheme2, configDNS2.getScheme(), DEFAULT_SCHEME);
         * setTextFieldValue(tfIp2, configDNS2.getIPAddress());
         * setTextFieldValue(tfPort2, String.valueOf(configDNS2.getPort()));
 * setTextFieldValue(tfAuth2, configDNS2.password());
         * } else {
         * log("DNS2 config is null, using defaults");
         * setComboBoxValue(comboBoxScheme2, DEFAULT_SCHEME, DEFAULT_SCHEME);
         * setTextFieldValue(tfIp2, "");
         * }
         */

        // Populate widget settings
        if (widgetConfig != null) {
            log("Loading widget config: size=" + widgetConfig.getSize() + ", layout=" + widgetConfig.getLayout()
                    + ", theme=" + widgetConfig.getTheme());
            setComboBoxValue(comboBoxSize, widgetConfig.getSize(), DEFAULT_SIZE);
            setComboBoxValue(comboBoxLayout, widgetConfig.getLayout(), DEFAULT_LAYOUT);
            setComboBoxValue(comboBoxTheme, widgetConfig.getTheme(), DEFAULT_THEME);
            setTextFieldValue(tfUpdateStatus, String.valueOf(widgetConfig.getUpdate_status_sec()));
            setTextFieldValue(tfUpdateFluid, String.valueOf(widgetConfig.getUpdate_fluid_sec()));
            setTextFieldValue(tfUpdateActive, String.valueOf(widgetConfig.getUpdate_active_sec()));
            setTextFieldValue(tfUpdateTopX, String.valueOf(widgetConfig.getUpdate_topx_sec()));
            setTextFieldValue(tfTopXCount, String.valueOf(widgetConfig.topX()));
        } else {
            setTextFieldValue(tfUpdateStatus, String.valueOf(DEFAULT_UPDATE_STATUS_SEC));
            setTextFieldValue(tfUpdateFluid, String.valueOf(DEFAULT_UPDATE_FLUID_SEC));
            setTextFieldValue(tfUpdateActive, String.valueOf(DEFAULT_UPDATE_ACTIVE_SEC));
            setTextFieldValue(tfUpdateTopX, String.valueOf(DEFAULT_UPDATE_TOPX_SEC));
            setTextFieldValue(tfTopXCount, String.valueOf(DEFAULT_TOPX_COUNT));
        }

        log("Configuration loaded");
    }

    // ==================== Notifications ====================

    private void showInfoAlert(String title, String content) {
        Runnable showTask = () -> {
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);

            var owner = findOwnerWindow();
            if (owner != null) {
                alert.initOwner(owner);
            }

            alert.show();
        };

        if (Platform.isFxApplicationThread()) {
            showTask.run();
        } else {
            Platform.runLater(showTask);
        }
    }

    private Window findOwnerWindow() {
        if (accord != null && accord.getScene() != null) {
            return accord.getScene().getWindow();
        }
        if (buttonApply != null && buttonApply.getScene() != null) {
            return buttonApply.getScene().getWindow();
        }
        if (buttonSave != null && buttonSave.getScene() != null) {
            return buttonSave.getScene().getWindow();
        }
        if (buttonLoad != null && buttonLoad.getScene() != null) {
            return buttonLoad.getScene().getWindow();
        }
        return null;
    }

    // ==================== Utility Methods ====================

    private int parsePort(TextField field) {
        if (field == null)
            return DEFAULT_PORT;
        String text = field.getText();
        if (text == null || text.isBlank())
            return DEFAULT_PORT;
        try {
            int port = Integer.parseInt(text.trim());
            return (port > 0 && port <= 65535) ? port : DEFAULT_PORT;
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    private int parseInterval(TextField field, int defaultValue) {
        if (field == null)
            return defaultValue;
        String text = field.getText();
        if (text == null || text.isBlank())
            return defaultValue;
        try {
            int interval = Integer.parseInt(text.trim());
            return interval > 0 ? interval : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getTextOrEmpty(TextField field) {
        return (field != null && field.getText() != null) ? field.getText().trim() : "";
    }

    private String getSelectedOrDefault(ComboBox<String> comboBox, String defaultValue) {
        if (comboBox == null || comboBox.getValue() == null)
            return defaultValue;
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

    private record ParsedHostInput(boolean valid, String host, Integer port, String scheme, String errorMessage) {
    }

    /**
     * Parses a "host" text field that may contain a URL or host:port.
     *
     * Rules:
     * - Allows host, host:port, http(s)://host[:port]
     * - Rejects any path/query/fragment
     */
    private ParsedHostInput parseHostInput(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedHostInput(true, "", null, null, "");
        }

        String text = raw.trim();

        // If it looks like a URL (has scheme or contains / ? #), parse as URI.
        boolean looksLikeUrl = text.contains("://") || text.contains("/") || text.contains("?") || text.contains("#");
        if (looksLikeUrl) {
            URI uri;
            try {
                // If scheme is missing but it contains path-like chars, add a dummy scheme for parsing.
                if (!text.contains("://")) {
                    uri = URI.create("http://" + text);
                } else {
                    uri = URI.create(text);
                }
            } catch (Exception e) {
                return new ParsedHostInput(false, "", null, null, "Host must be a hostname/IP (optionally with :port).");
            }

            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();

            // Handle cases like "http://pi.hole" where host is in authority.
            if (host == null && uri.getRawAuthority() != null) {
                // rawAuthority may be "host:port"
                ParsedHostInput hp = parseHostPort(uri.getRawAuthority());
                if (!hp.valid()) {
                    return hp;
                }
                host = hp.host();
                port = hp.port() != null ? hp.port() : -1;
            }

            if (host == null || host.isBlank()) {
                return new ParsedHostInput(false, "", null, null, "Host must be a hostname/IP (optionally with :port).");
            }

            String path = uri.getPath();
            if (path != null && !path.isBlank() && !"/".equals(path)) {
                return new ParsedHostInput(false, "", null, null,
                        "Please enter only a hostname/IP (no path like " + path + ").");
            }
            if (uri.getQuery() != null || uri.getFragment() != null) {
                return new ParsedHostInput(false, "", null, null,
                        "Please enter only a hostname/IP (no query or fragment).");
            }

            String normalizedScheme = (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
                    ? scheme.toLowerCase()
                    : null;
            Integer parsedPort = (port > 0 && port <= 65535) ? port : null;

            return new ParsedHostInput(true, host, parsedPort, normalizedScheme, "");
        }

        // Otherwise treat as host[:port] (no path).
        return parseHostPort(text);
    }

    private ParsedHostInput parseHostPort(String authority) {
        if (authority == null || authority.isBlank()) {
            return new ParsedHostInput(true, "", null, null, "");
        }
        String s = authority.trim();

        // Bracketed IPv6: [::1]:8080 or [::1]
        if (s.startsWith("[") && s.contains("]")) {
            int end = s.indexOf(']');
            String host = s.substring(1, end);
            if (host.isBlank()) {
                return new ParsedHostInput(false, "", null, null, "Invalid IPv6 host.");
            }
            if (end == s.length() - 1) {
                return new ParsedHostInput(true, host, null, null, "");
            }
            if (s.length() > end + 2 && s.charAt(end + 1) == ':') {
                String portPart = s.substring(end + 2);
                Integer p = parsePortString(portPart);
                if (p == null) {
                    return new ParsedHostInput(false, "", null, null, "Invalid port: " + portPart);
                }
                return new ParsedHostInput(true, host, p, null, "");
            }
            return new ParsedHostInput(false, "", null, null, "Invalid host format.");
        }

        // Unbracketed: try "host:port" using last colon (avoid breaking IPv6 which has many colons).
        int firstColon = s.indexOf(':');
        int lastColon = s.lastIndexOf(':');
        if (firstColon != -1 && firstColon == lastColon) {
            String host = s.substring(0, lastColon).trim();
            String portPart = s.substring(lastColon + 1).trim();
            if (host.isBlank()) {
                return new ParsedHostInput(false, "", null, null, "Host is required before :port.");
            }
            Integer p = parsePortString(portPart);
            if (p == null) {
                return new ParsedHostInput(false, "", null, null, "Invalid port: " + portPart);
            }
            return new ParsedHostInput(true, host, p, null, "");
        }

        // If there are multiple colons, assume it's an IPv6 literal without port.
        return new ParsedHostInput(true, s, null, null, "");
    }

    private Integer parsePortString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            int p = Integer.parseInt(text.trim());
            return (p > 0 && p <= 65535) ? p : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
