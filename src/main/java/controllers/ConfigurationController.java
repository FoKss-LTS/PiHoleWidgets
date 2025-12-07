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
    private ComboBox<String> ComboBoxSize,ComboBoxLayout;


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

        accord.setExpandedPane(dns1TitledPane);
        button_apply.setOnMouseClicked(event -> {
            saveConfiguration();
            WidgetApplication.applyAndCloseConfigurationWindow();
        });
        button_save.setOnMouseClicked(event -> saveConfiguration());
        button_load.setOnMouseClicked(event -> loadConfiguration());
        button_cancel.setOnMouseClicked(event -> WidgetApplication.closeConfigurationWindow());


        loadConfiguration();
    }


    @FXML
    public void saveConfiguration() {

        ConfigurationService confService = new ConfigurationService();

        int port1= TF_Port1.getText() != null ? Integer.parseInt(TF_Port1.getText()) :80;
        int port2= TF_Port2.getText() != null ? Integer.parseInt(TF_Port2.getText()) :80;

        String scheme1 = extractSchemeOrDefault(TF_IP1.getText());
        String scheme2 = extractSchemeOrDefault(TF_IP2.getText());
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
            TF_IP1.setText(formatHost(configDNS1));
            TF_Port1.setText(String.valueOf((configDNS1.getPort())));
            TF_AUTH1.setText(configDNS1.getAUTH());
        }

        if(configDNS2!=null) {
            TF_IP2.setText(formatHost(configDNS2));
            TF_Port2.setText(String.valueOf((configDNS2.getPort())));
            TG_AUTH2.setText(configDNS2.getAUTH());
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

    private String formatHost(PiholeConfig config) {
        if (config == null) return "";
        String scheme = config.getScheme() == null || config.getScheme().isEmpty() ? "http" : config.getScheme();
        String ip = config.getIPAddress() == null ? "" : config.getIPAddress();
        if (ip.isEmpty()) return "";
        return scheme + "://" + ip;
    }


}
