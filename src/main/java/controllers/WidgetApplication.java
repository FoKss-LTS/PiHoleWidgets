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

import domain.configuration.PiholeConfig;
import domain.configuration.WidgetConfig;
import helpers.ThemeManager;
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

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
        widgetStage.setOnShowing(_ -> refreshWidgetTiles());

        // Setup configuration stage
        configurationStage = new Stage();
        configurationStage.initOwner(widgetStage);
        configurationStage.initStyle(StageStyle.UNDECORATED);
        log("Configuration stage created");

        // Load configuration
        configService = new ConfigurationService();
        configService.readConfiguration();

        configDNS1 = configService.getConfigDNS1();
        widgetConfig = configService.getWidgetConfig();
        log("Configuration loaded - DNS1: " + (configDNS1 != null ? configDNS1.getIPAddress() : "null"));

        // Initialize configuration controller and view
        ConfigurationController configurationController = new ConfigurationController(configDNS1, widgetConfig);
        FXMLLoader configLoader = new FXMLLoader(getClass().getResource("Configuration.fxml"));
        configLoader.setController(configurationController);
        configurationRoot = configLoader.load();
        log("Configuration view loaded");

        // Initialize widget controller and view
        widgetController = new WidgetController(configDNS1, widgetConfig);
        FXMLLoader widgetLoader = new FXMLLoader(getClass().getResource("WidgetContainer.fxml"));
        widgetLoader.setController(widgetController);
        Parent widgetRoot = widgetLoader.load();
        log("Widget view loaded");

        // Create widget scene
        Scene widgetScene = new Scene(widgetController.getGridPane());
        
        // Apply theme to widget scene
        String theme = widgetConfig != null ? widgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;
        ThemeManager.applyTheme(widgetScene, theme);
        log("Applied theme: " + theme);
        
        // Setup drag handlers for widget
        setupDragHandlers(widgetRoot);
        
        widgetStage.setScene(widgetScene);
        
        // Handle window close event to minimize to tray instead of exiting
        widgetStage.setOnCloseRequest((WindowEvent event) -> {
            event.consume();
            hideToTray();
        });
        
        // Show the widget stage
        showWidget();
        log("Widget stage shown");

        // Setup and show configuration stage (initially hidden)
        Scene configScene = new Scene(configurationRoot);
        ThemeManager.applyTheme(configScene, theme);
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
        // DNS2 support intentionally disabled.
        // boolean dns2Valid = configDNS2 != null && configDNS2.hasValidAddress();
        // return dns1Valid || dns2Valid;
        return dns1Valid;
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
        if (configurationStage == null) return;

        configurationStage.setOpacity(1);
        configurationStage.show();
        bringStageToFront(configurationStage);
    }

    public static void applyAndCloseConfigurationWindow() {
        log("Applying configuration and closing window");
        
        if (configurationStage != null) {
            configurationStage.setOpacity(0);
        }
        
        // Reload configuration
        configService.readConfiguration();
        configDNS1 = configService.getConfigDNS1();
        widgetConfig = configService.getWidgetConfig();
        
        // Apply theme to both scenes
        String theme = widgetConfig != null ? widgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;
        if (widgetStage != null && widgetStage.getScene() != null) {
            ThemeManager.applyTheme(widgetStage.getScene(), theme);
        }
        if (configurationStage != null && configurationStage.getScene() != null) {
            ThemeManager.applyTheme(configurationStage.getScene(), theme);
        }
        
        // Update widget controller
        if (widgetController != null) {
            widgetController.setConfigDNS1(configDNS1);
            widgetController.setWidgetConfig(widgetConfig);
            widgetController.refreshPihole();
            widgetController.applyTheme(theme);
        }
        
        log("Configuration applied with theme: " + theme);
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
        trayIcon.addActionListener(_ -> Platform.runLater(WidgetApplication::showWidget));

        try {
            systemTray.add(trayIcon);
            log("System tray icon added");
        } catch (AWTException e) {
            logError("Unable to add tray icon", e);
        }
    }
    
    private static Image loadTrayIcon() {
        // Try multiple resource paths in order of preference
        // PNG format is more reliably loaded than ICO
        String[] iconPaths = {
            "/controllers/icon.png",       // PNG version (preferred)
            "/media/icons/icon.png",       // PNG in media folder
            "/controllers/icon.ico",       // ICO version
            "/media/icons/icon.ico",       // ICO in media folder
            "icon.png",                    // Relative PNG
            "icon.ico"                     // Relative ICO
        };
        
        logInfo("Attempting to load tray icon from " + iconPaths.length + " paths...");
        
        for (String path : iconPaths) {
            Image image = tryLoadIcon(path);
            if (image != null) {
                logInfo("Tray icon loaded successfully from: " + path);
                return image;
            }
        }
        
        // Fallback to programmatic icon
        logInfo("Could not load tray icon from any path, using programmatic fallback icon");
        return createFallbackTrayIcon();
    }
    
    private static Image tryLoadIcon(String resourcePath) {
        // First, try ImageIO (works for PNG, GIF, JPEG)
        Image image = tryLoadIconWithImageIO(resourcePath);
        if (image != null) {
            return image;
        }
        
        // Fallback to AWT Toolkit (may work for ICO on Windows)
        return tryLoadIconWithToolkit(resourcePath);
    }
    
    private static Image tryLoadIconWithImageIO(String resourcePath) {
        try {
            InputStream stream = WidgetApplication.class.getResourceAsStream(resourcePath);
            if (stream == null) {
                log("ImageIO: Resource not found at: " + resourcePath);
                return null;
            }
            
            try (stream) {
                BufferedImage image = ImageIO.read(stream);
                if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
                    logInfo("Loaded icon via ImageIO from: " + resourcePath + " (" + image.getWidth() + "x" + image.getHeight() + ")");
                    return scaleImage(image, 16, 16);
                } else {
                    log("ImageIO: Image read returned null or invalid dimensions for: " + resourcePath);
                }
            }
        } catch (IOException e) {
            log("ImageIO failed for " + resourcePath + ": " + e.getMessage());
        }
        return null;
    }
    
    private static Image tryLoadIconWithToolkit(String resourcePath) {
        try {
            URL iconUrl = WidgetApplication.class.getResource(resourcePath);
            if (iconUrl == null) {
                log("Toolkit: URL not found at: " + resourcePath);
                return null;
            }
            
            log("Toolkit: Attempting to load from URL: " + iconUrl);
            Image image = java.awt.Toolkit.getDefaultToolkit().getImage(iconUrl);
            
            // Wait for image to fully load
            java.awt.MediaTracker tracker = new java.awt.MediaTracker(new java.awt.Container());
            tracker.addImage(image, 0);
            tracker.waitForAll(2000); // 2 second timeout
            
            // Check if image loaded successfully
            if (tracker.isErrorAny()) {
                log("Toolkit: MediaTracker reported error for: " + resourcePath);
                return null;
            }
            
            // Verify the image has valid dimensions
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            if (width > 0 && height > 0) {
                logInfo("Loaded icon via Toolkit from: " + resourcePath + " (" + width + "x" + height + ")");
                return image;
            } else {
                log("Toolkit: Invalid dimensions (" + width + "x" + height + ") from: " + resourcePath);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log("Toolkit: Interrupted while loading from: " + resourcePath);
        } catch (Exception e) {
            log("Toolkit exception for " + resourcePath + ": " + e.getMessage());
        }
        return null;
    }
    
    private static Image scaleImage(BufferedImage original, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                          java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }
    
    private static Image createFallbackTrayIcon() {
        // Use a larger size for better visibility on high-DPI displays
        // Windows typically uses 16x16 or 32x32, but larger works better
        int size = 32;
        BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bufferedImage.createGraphics();
        
        // Enable anti-aliasing for smoother rendering
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                          java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, 
                          java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Fill background with Pi-hole red color
        g.setColor(new Color(220, 53, 69)); // Pi-hole red
        g.fillRect(0, 0, size, size);
        
        // Draw a white circle in the center (like Pi-hole logo)
        int circleSize = size - 8;
        int offset = 4;
        g.setColor(Color.WHITE);
        g.fillOval(offset, offset, circleSize, circleSize);
        
        // Draw a smaller red circle inside (donut effect)
        int innerSize = circleSize - 8;
        int innerOffset = offset + 4;
        g.setColor(new Color(220, 53, 69));
        g.fillOval(innerOffset, innerOffset, innerSize, innerSize);
        
        // Draw "P" in the center
        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String text = "P";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);
        
        g.dispose();
        
        logInfo("Created fallback tray icon: " + size + "x" + size);
        return bufferedImage;
    }
    
    private static PopupMenu createTrayPopupMenu() {
        PopupMenu popup = new PopupMenu();
        
        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(_ -> Platform.runLater(WidgetApplication::showWidget));
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
        showWidget();
    }
    
    private static void showWidget() {
        if (widgetStage != null) {
            widgetStage.show();
            bringStageToFront(widgetStage);
        }
    }
    
    private static void refreshWidgetTiles() {
        if (widgetController != null) {
            widgetController.refreshAllTiles();
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
