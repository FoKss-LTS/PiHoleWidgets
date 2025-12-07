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

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main JavaFX Application class for the Pi-hole Widget.
 * Manages the widget window, configuration window, and system tray integration.
 */
public class WidgetApplication extends Application {

    // ==================== Logging ====================
    
    private static final Logger LOGGER = Logger.getLogger(WidgetApplication.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));
    
    private static void log(String message) {
        if (VERBOSE) {
            LOGGER.log(Level.FINE, () -> "[App] " + message);
        }
    }
    
    private static void logInfo(String message) {
        LOGGER.log(Level.INFO, () -> message);
    }
    
    private static void logError(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }

    // ==================== Instance Fields ====================
    
    private double xOffset;
    private double yOffset;

    // ==================== Static Application State ====================
    
    private static PiholeConfig configDNS1;
    private static PiholeConfig configDNS2;
    private static WidgetConfig widgetConfig;
    
    private static Parent configurationRoot;
    private static ConfigurationService configService;
    private static Stage configurationStage;
    private static Stage widgetStage;
    private static WidgetController widgetController;
    private static SystemTray systemTray;
    private static TrayIcon trayIcon;

    // ==================== Application Lifecycle ====================

    @Override
    public void start(Stage primaryStage) throws IOException {
        log("=== Starting WidgetApplication ===");
        
        // Initialize system tray
        initializeSystemTray();

        // Setup main widget stage
        widgetStage = primaryStage;
        widgetStage.setTitle("PiHole Widget");
        widgetStage.initStyle(StageStyle.UNDECORATED);
        log("Widget stage created");

        // Setup configuration stage
        configurationStage = new Stage();
        configurationStage.initOwner(widgetStage);
        configurationStage.initStyle(StageStyle.UNDECORATED);
        log("Configuration stage created");

        // Load configuration
        configService = new ConfigurationService();
        configService.readConfiguration();

        configDNS1 = configService.getConfigDNS1();
        configDNS2 = configService.getConfigDNS2();
        widgetConfig = configService.getWidgetConfig();
        log("Configuration loaded - DNS1: " + (configDNS1 != null ? configDNS1.getIPAddress() : "null"));

        // Initialize configuration controller and view
        ConfigurationController configurationController = new ConfigurationController(configDNS1, configDNS2, widgetConfig);
        FXMLLoader configLoader = new FXMLLoader(getClass().getResource("Configuration.fxml"));
        configLoader.setController(configurationController);
        configurationRoot = configLoader.load();
        log("Configuration view loaded");

        // Initialize widget controller and view
        widgetController = new WidgetController(configDNS1, configDNS2, widgetConfig);
        FXMLLoader widgetLoader = new FXMLLoader(getClass().getResource("WidgetContainer.fxml"));
        widgetLoader.setController(widgetController);
        Parent widgetRoot = widgetLoader.load();
        log("Widget view loaded");

        // Create widget scene
        Scene widgetScene = new Scene(widgetController.getGridPane());
        
        // Setup drag handlers for widget
        setupDragHandlers(widgetRoot);
        
        widgetStage.setScene(widgetScene);
        
        // Handle window close event to minimize to tray instead of exiting
        widgetStage.setOnCloseRequest((WindowEvent event) -> {
            event.consume();
            hideToTray();
        });
        
        // Show the widget stage
        widgetStage.show();
        bringStageToFront(widgetStage);
        log("Widget stage shown");

        // Setup and show configuration stage (initially hidden)
        Scene configScene = new Scene(configurationRoot);
        configurationStage.setScene(configScene);
        configurationStage.setOpacity(0);
        configurationStage.setAlwaysOnTop(true);
        configurationStage.show();

        // Open configuration if no valid DNS is configured
        if (!hasValidDnsConfig()) {
            log("No valid DNS configuration found, opening configuration window");
            openConfigurationWindow();
        }
        
        log("=== WidgetApplication started ===");
    }
    
    private boolean hasValidDnsConfig() {
        boolean dns1Valid = configDNS1 != null && configDNS1.hasValidAddress();
        boolean dns2Valid = configDNS2 != null && configDNS2.hasValidAddress();
        return dns1Valid || dns2Valid;
    }
    
    private void setupDragHandlers(Parent root) {
        // Setup drag handlers for grid pane children
        for (Node node : widgetController.getGridPane().getChildren()) {
            node.setOnMousePressed(event -> {
                xOffset = widgetStage.getX() - event.getScreenX();
                yOffset = widgetStage.getY() - event.getScreenY();
            });
            node.setOnMouseDragged(event -> {
                widgetStage.setX(event.getScreenX() + xOffset);
                widgetStage.setY(event.getScreenY() + yOffset);
            });
        }

        // Setup drag handlers for root
        root.setOnMousePressed(event -> {
            xOffset = widgetStage.getX() - event.getScreenX();
            yOffset = widgetStage.getY() - event.getScreenY();
        });
        root.setOnMouseDragged(event -> {
            widgetStage.setX(event.getScreenX() + xOffset);
            widgetStage.setY(event.getScreenY() + yOffset);
        });
    }

    // ==================== Configuration Window Management ====================

    public static void openConfigurationWindow() {
        log("Opening configuration window");
        if (configurationStage != null) {
            configurationStage.setOpacity(1);
        }
    }

    public static void applyAndCloseConfigurationWindow() {
        log("Applying configuration and closing window");
        
        if (configurationStage != null) {
            configurationStage.setOpacity(0);
        }
        
        // Reload configuration
        configService.readConfiguration();
        configDNS1 = configService.getConfigDNS1();
        configDNS2 = configService.getConfigDNS2();
        widgetConfig = configService.getWidgetConfig();
        
        // Update widget controller
        if (widgetController != null) {
            widgetController.setConfigDNS1(configDNS1);
            widgetController.setConfigDNS2(configDNS2);
            widgetController.setWidgetConfig(widgetConfig);
            widgetController.refreshPihole();
        }
        
        log("Configuration applied");
    }

    public static void closeConfigurationWindow() {
        log("Closing configuration window");
        if (configurationStage != null) {
            configurationStage.setOpacity(0);
        }
    }

    // ==================== System Tray ====================

    private static void initializeSystemTray() {
        log("Initializing system tray");
        
        if (!SystemTray.isSupported()) {
            logInfo("System tray is not supported on this platform");
            return;
        }

        Platform.setImplicitExit(false);
        systemTray = SystemTray.getSystemTray();

        // Load tray icon
        Image trayImage = loadTrayIcon();
        
        // Create popup menu
        PopupMenu popup = createTrayPopupMenu();

        // Create and configure tray icon
        trayIcon = new TrayIcon(trayImage, "PiHole Widget", popup);
        trayIcon.setImageAutoSize(true);
        
        // Double-click to show window
        trayIcon.addActionListener(_ -> Platform.runLater(() -> {
            if (widgetStage != null) {
                widgetStage.show();
                bringStageToFront(widgetStage);
            }
        }));

        try {
            systemTray.add(trayIcon);
            log("System tray icon added");
        } catch (AWTException e) {
            logError("Unable to add tray icon", e);
        }
    }
    
    private static Image loadTrayIcon() {
        Image image = null;
        
        try {
            // Try multiple possible resource paths
            URL iconUrl = WidgetApplication.class.getResource("/media/icons/icon.ico");
            if (iconUrl == null) {
                iconUrl = WidgetApplication.class.getClassLoader().getResource("media/icons/icon.ico");
            }
            if (iconUrl == null) {
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
                log("Tray icon loaded from " + iconUrl);
            } else {
                log("Tray icon not found at /media/icons/icon.ico");
            }
        } catch (Exception e) {
            logError("Could not load tray icon", e);
        }
        
        // Create fallback icon if loading failed
        if (image == null) {
            log("Creating fallback tray icon");
            image = createFallbackTrayIcon();
        }
        
        return image;
    }
    
    private static Image createFallbackTrayIcon() {
        BufferedImage bufferedImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bufferedImage.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 16, 16);
        g.setColor(Color.WHITE);
        g.drawString("PH", 2, 12);
        g.dispose();
        return bufferedImage;
    }
    
    private static PopupMenu createTrayPopupMenu() {
        PopupMenu popup = new PopupMenu();
        
        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(_ -> Platform.runLater(() -> {
            if (widgetStage != null) {
                widgetStage.show();
                bringStageToFront(widgetStage);
            }
        }));
        popup.add(showItem);

        MenuItem hideItem = new MenuItem("Hide to Tray");
        hideItem.addActionListener(_ -> Platform.runLater(WidgetApplication::hideToTray));
        popup.add(hideItem);

        popup.addSeparator();

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.addActionListener(_ -> Platform.runLater(WidgetApplication::openConfigurationWindow));
        popup.add(settingsItem);

        popup.addSeparator();

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(_ -> Platform.runLater(() -> {
            log("Exit requested from tray");
            cleanup();
            Platform.exit();
            System.exit(0);
        }));
        popup.add(exitItem);
        
        return popup;
    }

    public static void hideToTray() {
        log("Hiding to tray");
        if (widgetStage != null) {
            widgetStage.hide();
        }
    }

    public static void showFromTray() {
        log("Showing from tray");
        if (widgetStage != null) {
            widgetStage.show();
            bringStageToFront(widgetStage);
        }
    }
    
    /**
     * Reliably brings a stage to the front on Windows.
     * Windows doesn't always honor toFront() calls, so we use the "always on top" trick.
     */
    private static void bringStageToFront(Stage stage) {
        if (stage == null) return;
        
        Platform.runLater(() -> {
            stage.setAlwaysOnTop(true);
            stage.toFront();
            stage.requestFocus();
            
            // Reset always on top after a brief delay
            Platform.runLater(() -> stage.setAlwaysOnTop(false));
        });
    }
    
    /**
     * Cleanup resources before exit.
     */
    private static void cleanup() {
        log("Cleaning up resources");
        
        // Shutdown widget controller schedulers
        if (widgetController != null) {
            widgetController.shutdown();
        }
        
        // Remove tray icon
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }

    // ==================== Main Entry Point ====================

    public static void main(String[] args) {
        launch();
    }
}
