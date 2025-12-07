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
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import services.configuration.ConfigurationService;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;


public class WidgetApplication extends Application {

    private double xOffset;
    private double yOffset;

    static PiholeConfig configDNS1 ;
    static PiholeConfig configDNS2 ;
    static WidgetConfig widgetConfig;

    static Parent root2;
    static ConfigurationService confService;
    static Stage configurationStage;
    static Stage widgetStage;
    static WidgetController widgetController;
    private static SystemTray systemTray;
    private static TrayIcon trayIcon;


    @Override
    public void start(Stage primaryStage) throws IOException {
        // Initialize system tray
        initializeSystemTray();

        // Use primaryStage as widgetStage to show in taskbar
        widgetStage = primaryStage;
        widgetStage.setTitle("PiHole Widget");
        widgetStage.initStyle(StageStyle.UNDECORATED);

        configurationStage = new Stage();
        configurationStage.initOwner(widgetStage);
        configurationStage.initStyle(StageStyle.UNDECORATED);


        confService = new ConfigurationService();
        confService.readConfiguration();




        configDNS1 = confService.getConfigDNS1();
        configDNS2 = null;confService.getConfigDNS2();
        widgetConfig= confService.getWidgetConfig();


        ConfigurationController configurationController = new ConfigurationController(configDNS1, configDNS2,widgetConfig);

        FXMLLoader loader2 = new FXMLLoader(getClass().getResource("Configuration.fxml"));
        loader2.setController(configurationController);
        root2 = loader2.load();

        widgetController = new WidgetController(configDNS1, configDNS2,widgetConfig);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("WidgetContainer.fxml"));
        loader.setController(widgetController);
        Parent root = loader.load();

        Scene scene = new Scene(widgetController.getGridPane());

        /*
        for (Node truc : widgetController.rootPane.getChildren()) {

            truc.setOnMousePressed(event -> {
                xOffset = widgetStage.getX() - event.getScreenX();
                yOffset = widgetStage.getY() - event.getScreenY();
            });
            truc.setOnMouseDragged(event -> {
                widgetStage.setX(event.getScreenX() + xOffset);
                widgetStage.setY(event.getScreenY() + yOffset);
            });
        }

        root.setOnMousePressed(event -> {
            xOffset = widgetStage.getX() - event.getScreenX();
            yOffset = widgetStage.getY() - event.getScreenY();
        });
        root.setOnMouseDragged(event -> {
            widgetStage.setX(event.getScreenX() + xOffset);
            widgetStage.setY(event.getScreenY() + yOffset);
        });

        root.setOnMousePressed(event -> {
            xOffset = widgetStage.getX() - event.getScreenX();
            yOffset = widgetStage.getY() - event.getScreenY();
        });
        root.setOnMouseDragged(event -> {
            widgetStage.setX(event.getScreenX() + xOffset);
            widgetStage.setY(event.getScreenY() + yOffset);
        });
        */

        for (Node truc : widgetController.getGridPane().getChildren()) {

            truc.setOnMousePressed(event -> {
                xOffset = widgetStage.getX() - event.getScreenX();
                yOffset = widgetStage.getY() - event.getScreenY();
            });
            truc.setOnMouseDragged(event -> {
                widgetStage.setX(event.getScreenX() + xOffset);
                widgetStage.setY(event.getScreenY() + yOffset);
            });
        }

        root.setOnMousePressed(event -> {
            xOffset = widgetStage.getX() - event.getScreenX();
            yOffset = widgetStage.getY() - event.getScreenY();
        });
        root.setOnMouseDragged(event -> {
            widgetStage.setX(event.getScreenX() + xOffset);
            widgetStage.setY(event.getScreenY() + yOffset);
        });

        root.setOnMousePressed(event -> {
            xOffset = widgetStage.getX() - event.getScreenX();
            yOffset = widgetStage.getY() - event.getScreenY();
        });
        root.setOnMouseDragged(event -> {
            widgetStage.setX(event.getScreenX() + xOffset);
            widgetStage.setY(event.getScreenY() + yOffset);
        });


        widgetStage.setScene(scene);
        
        // Handle window close event to minimize to tray instead of exiting
        widgetStage.setOnCloseRequest((WindowEvent event) -> {
            event.consume();
            hideToTray();
        });
        
        widgetStage.show();

        Scene scene2 = new Scene(root2);
        configurationStage.setScene(scene2);
        configurationStage.setOpacity(0);
        configurationStage.setAlwaysOnTop(true);
        configurationStage.show();

        if ((configDNS1==null || configDNS1.getIPAddress().isEmpty()) && (configDNS2==null ||configDNS2.getIPAddress().isEmpty()))
        openConfigurationWindow();

        /*} else {
            configurationController = new ConfigurationController(configDNS1,configDNS2);

            loader2 = new FXMLLoader(getClass().getResource("Configuration.fxml"));
            loader2.setController(configurationController);
            Parent root2 = loader2.load();
            Scene scene2 = new Scene(root2);
            configurationStage.setScene(scene2);
            configurationStage.show();
          /* Alert alert = new Alert(Alert.AlertType.ERROR, "Please input your configuration before opening the widget", ButtonType.OK);
           alert.setHeaderText("Configuration Missing");
           alert.showAndWait();

            /*  if (alert.getResult() == ButtonType.OK) {
                System.exit(0);
            }
            System.exit(0);*/
    //}

    }

    public static void openConfigurationWindow()
    {
        configurationStage.setOpacity(1);
    }

    public static void applyAndCloseConfigurationWindow()
    {
        configurationStage.setOpacity(0);
        confService.readConfiguration();


        configDNS1 = confService.getConfigDNS1();
        configDNS2 = confService.getConfigDNS2();

        widgetConfig=confService.getWidgetConfig();

        widgetController.setConfigDNS1(configDNS1);
        widgetController.setConfigDNS2(configDNS2);
        widgetController.setWidgetConfig(widgetConfig);


        widgetController.refreshPihole();

    }

    public static void closeConfigurationWindow(){
        configurationStage.setOpacity(0);
    }

    private static void initializeSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported on this platform");
            return;
        }

        Platform.setImplicitExit(false);
        systemTray = SystemTray.getSystemTray();

        // Load icon for system tray
        Image image = null;
        try {
            // Try multiple possible paths
            URL iconUrl = WidgetApplication.class.getResource("/media/icons/icon.ico");
            if (iconUrl == null) {
                // Try alternative path
                iconUrl = WidgetApplication.class.getClassLoader().getResource("media/icons/icon.ico");
            }
            if (iconUrl == null) {
                // Try without leading slash
                iconUrl = WidgetApplication.class.getResource("media/icons/icon.ico");
            }
            
            if (iconUrl != null) {
                image = Toolkit.getDefaultToolkit().getImage(iconUrl);
                // Wait for image to load
                MediaTracker tracker = new MediaTracker(new java.awt.Container());
                tracker.addImage(image, 0);
                try {
                    tracker.waitForAll();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                System.err.println("Tray icon not found at /media/icons/icon.ico");
            }
        } catch (Exception e) {
            System.err.println("Could not load tray icon: " + e.getMessage());
            e.printStackTrace();
        }
        
        // If icon loading failed, create a simple default icon
        if (image == null) {
            System.err.println("Creating default tray icon");
            // Create a simple 16x16 colored icon as fallback
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = bufferedImage.createGraphics();
            g.setColor(java.awt.Color.BLUE);
            g.fillRect(0, 0, 16, 16);
            g.setColor(java.awt.Color.WHITE);
            g.drawString("PH", 2, 12);
            g.dispose();
            image = bufferedImage;
        }

        // Create popup menu
        PopupMenu popup = new PopupMenu();
        
        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Platform.runLater(() -> {
                    if (widgetStage != null) {
                        widgetStage.show();
                        widgetStage.toFront();
                    }
                });
            }
        });
        popup.add(showItem);

        MenuItem hideItem = new MenuItem("Hide to Tray");
        hideItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Platform.runLater(() -> {
                    hideToTray();
                });
            }
        });
        popup.add(hideItem);

        popup.addSeparator();

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Platform.runLater(() -> {
                    openConfigurationWindow();
                });
            }
        });
        popup.add(settingsItem);

        popup.addSeparator();

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Platform.runLater(() -> {
                    if (systemTray != null && trayIcon != null) {
                        systemTray.remove(trayIcon);
                    }
                    Platform.exit();
                    System.exit(0);
                });
            }
        });
        popup.add(exitItem);

        // Create tray icon
        trayIcon = new TrayIcon(image, "PiHole Widget", popup);
        trayIcon.setImageAutoSize(true);
        
        // Add double-click listener to show window
        trayIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Platform.runLater(() -> {
                    if (widgetStage != null) {
                        widgetStage.show();
                        widgetStage.toFront();
                    }
                });
            }
        });

        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Unable to add tray icon: " + e.getMessage());
        }
    }

    public static void hideToTray() {
        if (widgetStage != null) {
            widgetStage.hide();
        }
    }

    public static void showFromTray() {
        if (widgetStage != null) {
            widgetStage.show();
            widgetStage.toFront();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}