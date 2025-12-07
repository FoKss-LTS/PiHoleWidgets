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
import javafx.scene.control.*;
import services.configuration.ConfigurationService;

import java.net.URL;
import java.util.ResourceBundle;


public class ConfigurationController implements Initializable {

    @FXML
    private Button button_cancel, button_save, button_load, button_apply;

    @FXML
    private TitledPane dns1TitledPane;
    @FXML
    private Accordion accord;

    @FXML
    private TextField TF_IP1,TF_Port1, TF_IP2,TF_Port2, TF_AUTH1, TG_AUTH2;

    @FXML
    private ComboBox<String> ComboBoxSize,ComboBoxLayout, ComboBox_Scheme1, ComboBox_Scheme2;


    private PiholeConfig configDNS1;
    private PiholeConfig configDNS2;
    private WidgetConfig widgetConfig;


    public ConfigurationController(PiholeConfig configDNS1, PiholeConfig configDNS2,WidgetConfig widgetConfig) {
        this.configDNS1 = configDNS1;
        this.configDNS2 = configDNS2;
        this.widgetConfig = widgetConfig;
    }

    public void initialize(URL location, ResourceBundle resources) {
        String sizes[] =
                { "Small", "Medium", "Large",
                        "XXL","Full Screen" };
        ComboBoxSize.setItems(FXCollections
                .observableArrayList(sizes));

        String layouts[] =
                { "Horizontal",/* "Vertical",*/ "Square" };
        ComboBoxLayout.setItems(FXCollections
                .observableArrayList(layouts));

        // Initialize scheme ComboBoxes
        String schemes[] = { "http", "https" };
        ComboBox_Scheme1.setItems(FXCollections.observableArrayList(schemes));
        ComboBox_Scheme2.setItems(FXCollections.observableArrayList(schemes));
        ComboBox_Scheme1.setValue("http");
        ComboBox_Scheme2.setValue("http");
        
        // Apply dark theme styling to all ComboBoxes
        applyComboBoxDarkTheme(ComboBox_Scheme1);
        applyComboBoxDarkTheme(ComboBox_Scheme2);
        applyComboBoxDarkTheme(ComboBoxSize);
        applyComboBoxDarkTheme(ComboBoxLayout);

        accord.setExpandedPane(dns1TitledPane);
        button_apply.setOnMouseClicked(event -> {
            saveConfiguration();
            WidgetApplication.applyAndCloseConfigurationWindow();
        });
        button_save.setOnMouseClicked(event -> saveConfiguration());
        button_load.setOnMouseClicked(event -> loadConfiguration());
        button_cancel.setOnMouseClicked(event -> WidgetApplication.closeConfigurationWindow());

        // Add button hover effects
        setupButtonHoverEffects();

        loadConfiguration();
    }

    private void setupButtonHoverEffects() {
        // Apply button
        button_apply.setOnMouseEntered(e -> button_apply.setStyle("-fx-background-color: #5aaeff; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;"));
        button_apply.setOnMouseExited(e -> button_apply.setStyle("-fx-background-color: #4a9eff; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;"));

        // Load button
        button_load.setOnMouseEntered(e -> button_load.setStyle("-fx-background-color: #7c858d; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;"));
        button_load.setOnMouseExited(e -> button_load.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;"));

        // Save button
        button_save.setOnMouseEntered(e -> button_save.setStyle("-fx-background-color: #38b755; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;"));
        button_save.setOnMouseExited(e -> button_save.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;"));

        // Cancel button
        button_cancel.setOnMouseEntered(e -> button_cancel.setStyle("-fx-background-color: #ec4555; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;"));
        button_cancel.setOnMouseExited(e -> button_cancel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16 8 16;"));
    }

    private void applyComboBoxDarkTheme(ComboBox<String> comboBox) {
        if (comboBox != null) {
            // Apply comprehensive dark theme styling for ComboBox
            String darkStyle = 
                "-fx-background-color: #2a2d32; " +
                "-fx-text-fill: #e0e0e0; " +
                "-fx-control-inner-background: #2a2d32; " +
                "-fx-prompt-text-fill: #888888;";
            comboBox.setStyle(darkStyle);
            
            // Set cell factory to ensure text is visible in dropdown
            comboBox.setCellFactory(listView -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: #2a2d32; -fx-text-fill: #e0e0e0;");
                    } else {
                        setText(item);
                        setStyle("-fx-background-color: #2a2d32; -fx-text-fill: #e0e0e0;");
                    }
                }
            });
            
            // Set button cell factory to ensure selected text is visible
            comboBox.setButtonCell(new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                    setStyle("-fx-background-color: #2a2d32; -fx-text-fill: #e0e0e0;");
                }
            });
        }
    }


    @FXML
    public void saveConfiguration() {

        ConfigurationService confService = new ConfigurationService();

        int port1= TF_Port1.getText() != null && !TF_Port1.getText().isEmpty() ? Integer.parseInt(TF_Port1.getText()) :80;
        int port2= TF_Port2.getText() != null && !TF_Port2.getText().isEmpty() ? Integer.parseInt(TF_Port2.getText()) :80;

        // Get scheme from ComboBox, fallback to extracting from IP field for backward compatibility
        String scheme1 = ComboBox_Scheme1.getValue() != null ? ComboBox_Scheme1.getValue() : extractSchemeOrDefault(TF_IP1.getText());
        String scheme2 = ComboBox_Scheme2.getValue() != null ? ComboBox_Scheme2.getValue() : extractSchemeOrDefault(TF_IP2.getText());
        
        // Strip scheme from IP field if present (for backward compatibility)
        String ip1 = stripScheme(TF_IP1.getText());
        String ip2 = stripScheme(TF_IP2.getText());

        confService.writeConfigFile(scheme1, ip1,port1, TF_AUTH1.getText(), scheme2, ip2,port2, TG_AUTH2.getText(),
                ComboBoxSize.getValue()==null? "Medium" : ComboBoxSize.getValue().toString(), ComboBoxLayout.getValue()== null ? "Square" : ComboBoxLayout.getValue().toString(), true,true,true,5,5,5);

    }

    @FXML
    public void loadConfiguration() {
        ConfigurationService confService = new ConfigurationService();
        confService.readConfiguration();

        configDNS1 = confService.getConfigDNS1();
        configDNS2 = confService.getConfigDNS2();
        widgetConfig=confService.getWidgetConfig();

        if(configDNS1!=null) {
            // Set scheme from config
            String scheme1 = configDNS1.getScheme() != null && !configDNS1.getScheme().isEmpty() ? configDNS1.getScheme() : "http";
            ComboBox_Scheme1.setValue(scheme1);
            // Set IP address without scheme
            TF_IP1.setText(configDNS1.getIPAddress() != null ? configDNS1.getIPAddress() : "");
            TF_Port1.setText(String.valueOf((configDNS1.getPort())));
            TF_AUTH1.setText(configDNS1.getAUTH());
        } else {
            ComboBox_Scheme1.setValue("http");
            TF_IP1.setText("");
        }

        if(configDNS2!=null) {
            // Set scheme from config
            String scheme2 = configDNS2.getScheme() != null && !configDNS2.getScheme().isEmpty() ? configDNS2.getScheme() : "http";
            ComboBox_Scheme2.setValue(scheme2);
            // Set IP address without scheme
            TF_IP2.setText(configDNS2.getIPAddress() != null ? configDNS2.getIPAddress() : "");
            TF_Port2.setText(String.valueOf((configDNS2.getPort())));
            TG_AUTH2.setText(configDNS2.getAUTH());
        } else {
            ComboBox_Scheme2.setValue("http");
            TF_IP2.setText("");
        }

        if(widgetConfig!=null) {
            ComboBoxSize.setValue(widgetConfig.getSize());
            ComboBoxLayout.setValue(widgetConfig.getLayout());
        }


    }

    private String extractSchemeOrDefault(String rawHost) {
        if (rawHost == null) return "http";
        String host = rawHost.trim().toLowerCase();
        if (host.startsWith("https://")) return "https";
        if (host.startsWith("http://")) return "http";
        return "http";
    }

    private String stripScheme(String rawHost) {
        if (rawHost == null) return "";
        String host = rawHost.trim();
        if (host.startsWith("https://")) host = host.substring("https://".length());
        else if (host.startsWith("http://")) host = host.substring("http://".length());
        if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
        return host;
    }

}
