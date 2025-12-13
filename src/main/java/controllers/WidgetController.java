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
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.addons.Indicator;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import helpers.HelperService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.util.Duration;
import services.pihole.PiHoleHandler;

import java.net.URL;
import java.time.Year;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Pi-hole Widget dashboard.
 * Manages the display and periodic refresh of Pi-hole statistics across multiple tiles.
 */
public class WidgetController implements Initializable {

    // ==================== Constants ====================
    
    private static final String WIDGET_VERSION = "1.5.2";
    private static final int DEFAULT_TOP_X = 5;
    private static final int DOMAIN_TRUNCATE_LENGTH = 20;
    private static final String TRUNCATION_SUFFIX = "..";
    private static final String RANK_ICON_PATH_PATTERN = "/media/images/%d.png";
    private static final int TOOLTIP_DELAY_MS = 200;
    
    // Scheduler intervals in seconds
    private static final long STATUS_REFRESH_INTERVAL = 5;
    private static final long FLUID_REFRESH_INTERVAL = 15;
    private static final long ACTIVE_REFRESH_INTERVAL = 60;
    private static final long TOPX_REFRESH_INTERVAL = 5;
    
    // Default tile dimensions
    private static final double DEFAULT_TILE_WIDTH = 200;
    private static final double DEFAULT_TILE_HEIGHT = 200;
    
    // ==================== Logging ====================
    
    private static final Logger LOGGER = Logger.getLogger(WidgetController.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));
    
    private static void log(String message) {
        if (VERBOSE) {
            LOGGER.log(Level.FINE, () -> "[Widget] " + message);
        }
    }
    
    private static void logInfo(String message) {
        LOGGER.log(Level.INFO, () -> "[Widget] " + message);
    }
    
    // ==================== Instance Fields ====================
    
    // Tile dimensions and layout
    private double tileWidth = DEFAULT_TILE_WIDTH;
    private double tileHeight = DEFAULT_TILE_HEIGHT;
    private int cols = 2;
    private int rows = 2;
    private int topX = DEFAULT_TOP_X;
    
    // UI Components
    private Tile statusTile;
    private Tile ledTile;
    private Tile fluidTile;
    private Tile topXTile;
    private VBox dataTable;
    private FlowGridPane gridPane;
    
    // Pi-hole handlers
    private PiHoleHandler piholeDns1;
    private PiHoleHandler piholeDns2;
    
    // Configuration
    private PiholeConfig configDNS1;
    private PiholeConfig configDNS2;
    private WidgetConfig widgetConfig;
    
    // Centralized scheduler for all periodic tasks
    private ScheduledExecutorService scheduler;
    
    // ==================== FXML Injected Fields ====================
    
    @FXML
    private Pane rootPane;
    
    @FXML
    private Label dakLabel;
    
    // ==================== Constructor ====================
    
    public WidgetController(PiholeConfig configDNS1, PiholeConfig configDNS2, WidgetConfig widgetConfig) {
        log("=== WidgetController constructor called ===");
        log("ConfigDNS1: " + formatConfig(configDNS1));
        log("ConfigDNS2: " + formatConfig(configDNS2));
        log("WidgetConfig: " + formatWidgetConfig(widgetConfig));
        
        this.configDNS1 = configDNS1;
        this.configDNS2 = configDNS2;
        this.widgetConfig = widgetConfig;
    }
    
    // ==================== Initialization ====================
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log("=== initialize() called ===");
        log("Location: " + location);
        log("ConfigDNS1 present: " + (configDNS1 != null));
        log("ConfigDNS2 present: " + (configDNS2 != null));
        
        if (configDNS1 == null && configDNS2 == null) {
            log("ERROR: Both configurations are null!");
            logInfo("configurations are empty");
            return;
        }
        
        log("At least one config is present, proceeding with initialization...");
        
        configureTileDimensions();
        configureLayout();
        
        log("Calling refreshPihole()...");
        refreshPihole();
        
        log("Calling initTiles()...");
        initTiles();
        
        log("Starting schedulers...");
        initializeSchedulers();
        log("All schedulers started");
        
        setupCopyrightLabel();
        setupGridPane();
        
        log("Initializing context menu...");
        initializeContextMenu();
        log("=== Widget initialization complete ===");
    }
    
    private void configureTileDimensions() {
        if (widgetConfig == null) {
            log("Widget config is null, using defaults");
            return;
        }
        
        log("Widget config size: " + widgetConfig.getSize());
        
        switch (widgetConfig.getSize()) {
            case "Small" -> {
                log("Setting tile size: Small (150x150)");
                tileWidth = 150;
                tileHeight = 150;
            }
            case "Medium" -> {
                log("Setting tile size: Medium (200x200)");
                tileWidth = DEFAULT_TILE_WIDTH;
                tileHeight = DEFAULT_TILE_HEIGHT;
            }
            case "Large" -> {
                log("Setting tile size: Large (350x350)");
                tileWidth = 350;
                tileHeight = 350;
            }
            case "XXL" -> {
                log("Setting tile size: XXL (500x500)");
                tileWidth = 500;
                tileHeight = 500;
            }
            case "Full Screen" -> {
                double screenWidth = Screen.getPrimary().getBounds().getMaxX();
                tileWidth = screenWidth / 4;
                tileHeight = screenWidth / 4;
                log("Setting tile size: Full Screen (" + tileWidth + "x" + tileHeight + ")");
            }
            default -> {
                log("Setting tile size: Default (200x200)");
                tileWidth = DEFAULT_TILE_WIDTH;
                tileHeight = DEFAULT_TILE_HEIGHT;
            }
        }
    }
    
    private void configureLayout() {
        if (widgetConfig == null) {
            log("Widget config is null, using default layout");
            return;
        }
        
        log("Widget config layout: " + widgetConfig.getLayout());
        
        switch (widgetConfig.getLayout()) {
            case "Horizontal" -> {
                cols = 4;
                rows = 1;
                log("Setting layout: Horizontal (4x1)");
            }
            case "Square" -> {
                cols = 2;
                rows = 2;
                log("Setting layout: Square (2x2)");
            }
            default -> {
                cols = 2;
                rows = 2;
                log("Setting layout: Default (2x2)");
            }
        }
    }
    
    private void setupCopyrightLabel() {
        String copyright = "Copyright (C) " + Year.now().getValue() + ".  Reda ELFARISSI aka foxy999";
        dakLabel.setText(copyright);
        dakLabel.setLayoutX(tileWidth + 1);
        dakLabel.setLayoutY((tileHeight * 2) - 15);
        dakLabel.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isPrimaryButtonDown()) {
                openConfigurationWindow();
            }
        });
    }
    
    private void setupGridPane() {
        log("Creating FlowGridPane with " + cols + " cols x " + rows + " rows");
        log("Adding tiles: ledTile, fluidTile, statusTile, topXTile");
        
        gridPane = new FlowGridPane(cols, rows, ledTile, fluidTile, statusTile, topXTile);
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setCenterShape(true);
        gridPane.setPadding(new Insets(5));
        gridPane.setBackground(new Background(new BackgroundFill(Color.web("#101214"), CornerRadii.EMPTY, Insets.EMPTY)));
        
        log("FlowGridPane created with background color #101214");
    }
    
    // ==================== Scheduler Management ====================
    
    private void initializeSchedulers() {
        log("Initializing centralized scheduler with virtual threads...");
        
        // Use virtual threads for lightweight scheduling (Java 21+ feature)
        scheduler = Executors.newScheduledThreadPool(4, Thread.ofVirtual()
                .name("pihole-widget-", 0)
                .factory());
        
        scheduler.scheduleAtFixedRate(this::inflateStatusData, 0, STATUS_REFRESH_INTERVAL, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::inflateActiveData, 0, ACTIVE_REFRESH_INTERVAL, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::inflateFluidData, 0, FLUID_REFRESH_INTERVAL, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::inflateTopXData, 0, TOPX_REFRESH_INTERVAL, TimeUnit.SECONDS);
        
        log("All schedulers initialized - Status: " + STATUS_REFRESH_INTERVAL + "s, " +
            "Active: " + ACTIVE_REFRESH_INTERVAL + "s, " +
            "Fluid: " + FLUID_REFRESH_INTERVAL + "s, " +
            "TopX: " + TOPX_REFRESH_INTERVAL + "s");
    }
    
    /**
     * Shuts down the scheduler gracefully. Should be called when the widget is closed.
     */
    public void shutdown() {
        log("Shutting down schedulers...");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log("Schedulers shut down");
    }
    
    // ==================== Pi-hole Data Management ====================
    
    public void refreshPihole() {
        log("=== refreshPihole() called ===");
        
        if (configDNS1 != null) {
            log("Creating PiHoleHandler for DNS1: " + configDNS1.getIPAddress() + ":" + configDNS1.getPort());
            piholeDns1 = new PiHoleHandler(
                configDNS1.getIPAddress(), 
                configDNS1.getPort(), 
                configDNS1.getScheme(), 
                configDNS1.getAUTH()
            );
            logInfo("PiHole DNS1 version: " + piholeDns1.getVersion());
            log("PiHoleHandler DNS1 created");
        } else {
            log("ConfigDNS1 is null, skipping DNS1 handler creation");
        }
        
        if (configDNS2 != null) {
            log("Creating PiHoleHandler for DNS2: " + configDNS2.getIPAddress() + ":" + configDNS2.getPort());
            piholeDns2 = new PiHoleHandler(
                configDNS2.getIPAddress(), 
                configDNS2.getPort(), 
                configDNS2.getScheme(), 
                configDNS2.getAUTH()
            );
            log("PiHoleHandler DNS2 created");
        } else {
            log("ConfigDNS2 is null, skipping DNS2 handler creation");
        }
        
        log("Calling inflateAllData()...");
        inflateAllData();
    }
    
    private void inflateAllData() {
        log("=== inflateAllData() called ===");
        log("Inflating active data...");
        inflateActiveData();
        log("Inflating fluid data...");
        inflateFluidData();
        log("Inflating status data...");
        inflateStatusData();
        log("Inflating topX data...");
        inflateTopXData();
        log("inflateAllData() complete");
    }
    
    /**
     * Fetches Pi-hole stats from both configured DNS handlers as JSON strings.
     * @return a record containing stats JSON from both Pi-holes (may be empty)
     */
    private PiholeStats fetchPiholeStats() {
        String pihole1 = (piholeDns1 != null) ? piholeDns1.getPiHoleStats() : "";
        String pihole2 = (piholeDns2 != null) ? piholeDns2.getPiHoleStats() : "";
        return new PiholeStats(pihole1, pihole2);
    }
    
    /**
     * Record to hold stats JSON from both Pi-hole instances.
     */
    private record PiholeStats(String pihole1, String pihole2) {
        boolean isActive1() {
            return pihole1 != null && !pihole1.isBlank();
        }
        
        boolean isActive2() {
            return pihole2 != null && !pihole2.isBlank();
        }
        
        boolean anyActive() {
            return isActive1() || isActive2();
        }
        
        boolean bothInactive() {
            return !anyActive();
        }
    }
    
    // ==================== Data Inflation Methods ====================
    
    public void inflateStatusData() {
        log("=== inflateStatusData() called ===");
        Platform.runLater(() -> {
            log("inflateStatusData - runLater executing...");
            
            PiholeStats stats = fetchPiholeStats();
            log("DNS1 stats: " + (stats.isActive1() ? "received" : "empty"));
            log("DNS2 stats: " + (stats.isActive2() ? "received" : "empty"));
            
            // TODO: Parse JSON stats when API implementation is complete
            // For now, using placeholder values
            long queries = 0L;
            long blockedAds = 0L;
            long queriesProcessed = 0L;
            long domainsBlocked = 0L;
            
            log("Updating status tile with values - Left: " + queries + ", Middle: " + blockedAds + ", Right: " + queriesProcessed);
            statusTile.setLeftValue(queries);
            statusTile.setMiddleValue(blockedAds);
            statusTile.setRightValue(queriesProcessed);
            statusTile.setDescription(HelperService.getHumanReadablePriceFromNumber(domainsBlocked));
            
            if (piholeDns1 != null) {
                String lastBlocked = piholeDns1.getLastBlocked();
                log("Last blocked: " + lastBlocked);
                statusTile.setText(lastBlocked);
            }
            
            log("inflateStatusData complete");
        });
    }
    
    public void inflateFluidData() {
        log("=== inflateFluidData() called ===");
        Platform.runLater(() -> {
            log("inflateFluidData - runLater executing...");
            
            PiholeStats stats = fetchPiholeStats();
            
            if (stats.bothInactive()) {
                log("WARNING: Both Pi-holes inactive, skipping fluid update");
                return;
            }
            
            // TODO: Parse JSON stats when API implementation is complete
            // For now, using placeholder values
            long queries = 0L;
            long blockedAds = 0L;
            
            double adsPercentage = 0.0;
            if (queries > 0L && blockedAds > 0L) {
                adsPercentage = (blockedAds / (double) queries) * 100.0;
            }
            
            log("Ads percentage calculated: " + adsPercentage + "% (queries: " + queries + ", blocked: " + blockedAds + ")");
            fluidTile.setValue(adsPercentage);
            
            if (piholeDns1 != null) {
                String gravityUpdate = piholeDns1.getGravityLastUpdate();
                log("Gravity last update: " + gravityUpdate);
                fluidTile.setText(gravityUpdate);
            }
            
            log("inflateFluidData complete");
        });
    }
    
    public void inflateActiveData() {
        log("=== inflateActiveData() called ===");
        Platform.runLater(() -> {
            log("inflateActiveData - runLater executing...");
            
            PiholeStats stats = fetchPiholeStats();
            log("DNS1 - Active: " + stats.isActive1());
            log("DNS2 - Active: " + stats.isActive2());
            
            // Handle case where both are inactive
            if (stats.bothInactive()) {
                log("WARNING: Both Pi-hole instances are inactive/unavailable - setting LED to RED");
                ledTile.setActiveColor(Color.RED);
                ledTile.setTitle("Widget Version: " + WIDGET_VERSION);
                ledTile.setDescription("No active Pi-hole");
                ledTile.setText("API Version: N/A");
                return;
            }
            
            // At least one is active - set LED to green
            log("At least one Pi-hole instance is active - setting LED to GREEN");
            ledTile.setActiveColor(Color.LIGHTGREEN);
            
            StringBuilder ips = new StringBuilder();
            String apiVersion = "";
            
            // Build IPs string and get API version
            if (stats.isActive1() && piholeDns1 != null) {
                ips.append(piholeDns1.getIPAddress());
                apiVersion = piholeDns1.getVersion();
            }
            
            if (stats.isActive2() && piholeDns2 != null) {
                if (!ips.isEmpty()) {
                    ips.append(" \n ");
                }
                ips.append(piholeDns2.getIPAddress());
                // Use second instance version if first was inactive
                if (!stats.isActive1()) {
                    apiVersion = piholeDns2.getVersion();
                }
            }
            
            log("Updating LED tile - IPS: " + ips + ", API Version: " + apiVersion);
            ledTile.setTitle("Widget Version: " + WIDGET_VERSION);
            ledTile.setDescription(ips.toString());
            ledTile.setText("API Version: " + apiVersion);
            ledTile.setTooltipText("Widget Version: " + WIDGET_VERSION);
            
            log("inflateActiveData complete");
        });
    }
    
    public void inflateTopXData() {
        log("=== inflateTopXData() called ===");
        Platform.runLater(() -> {
            log("inflateTopXData - runLater executing...");
            
            PiholeStats stats = fetchPiholeStats();
            
            if (stats.bothInactive()) {
                log("WARNING: Both Pi-holes inactive, skipping topX update");
                return;
            }
            
            if (piholeDns1 == null) {
                log("WARNING: piholeDns1 is null, skipping topX update");
                return;
            }
            
            log("Fetching top " + topX + " blocked domains...");
            String topBlockedJson = piholeDns1.getTopXBlocked(topX);
            
            if (topBlockedJson == null || topBlockedJson.isBlank()) {
                log("WARNING: topBlocked JSON is null or empty, skipping update");
                return;
            }
            
            // TODO: Parse JSON response when API implementation is complete
            // For now, just showing header with no data
            log("Received topBlocked JSON, parsing pending implementation");
            
            // Create header row
            HBox header = createTopXHeader();
            HBox spacerRow = createSpacerRow();
            
            dataTable.getChildren().setAll(header, spacerRow);
            log("DataTable header set, JSON parsing pending implementation");
            
            topXTile.setGraphic(dataTable);
            log("TopX tile graphic updated");
            log("inflateTopXData complete");
        });
    }
    
    // ==================== UI Component Creation ====================
    
    private HBox createTopXHeader() {
        Label nameLabel = new Label("Domain");
        nameLabel.setTextFill(Tile.FOREGROUND);
        nameLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.NEVER);
        
        Label blocksLabel = new Label("Nbr Blocks");
        blocksLabel.setTextFill(Tile.FOREGROUND);
        blocksLabel.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(blocksLabel, Priority.NEVER);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox header = new HBox(5, nameLabel, spacer, blocksLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setFillHeight(true);
        
        return header;
    }
    
    private HBox createSpacerRow() {
        Region spacer = new Region();
        spacer.setPrefSize(5, 5);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox spacerRow = new HBox(5, spacer);
        spacerRow.setAlignment(Pos.CENTER_LEFT);
        spacerRow.setFillHeight(true);
        
        return spacerRow;
    }
    
    private HBox createTopBlockedItem(int rank, String fullDomain, String truncatedDomain, String blockCount) {
        log("createTopBlockedItem() - Creating item #" + rank + ": " + truncatedDomain + " (" + blockCount + " blocks)");
        
        // Try to load rank icon from classpath resources
        ImageView iconView = loadRankIcon(rank);
        
        Label domainLabel = new Label(truncatedDomain);
        domainLabel.setTextFill(Tile.FOREGROUND);
        domainLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(domainLabel, Priority.NEVER);
        
        Region spacer = new Region();
        spacer.setPrefSize(5, 5);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label valueLabel = new Label(blockCount);
        valueLabel.setTextFill(Tile.FOREGROUND);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(valueLabel, Priority.NEVER);
        
        // Add tooltip with full domain name
        Tooltip tooltip = new Tooltip(fullDomain);
        tooltip.setShowDelay(Duration.millis(TOOLTIP_DELAY_MS));
        Tooltip.install(domainLabel, tooltip);
        
        HBox row;
        if (iconView != null) {
            row = new HBox(5, iconView, domainLabel, spacer, valueLabel);
        } else {
            row = new HBox(5, domainLabel, spacer, valueLabel);
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.setFillHeight(true);
        
        log("createTopBlockedItem() - Item #" + rank + " created");
        return row;
    }
    
    /**
     * Loads a rank icon from classpath resources.
     * @param rank the rank number (1-5)
     * @return ImageView with the icon, or null if not found
     */
    private ImageView loadRankIcon(int rank) {
        String resourcePath = RANK_ICON_PATH_PATTERN.formatted(rank);
        URL url = getClass().getResource(resourcePath);
        
        if (url != null) {
            try {
                Image image = new Image(url.toExternalForm());
                ImageView imageView = new ImageView(image);
                imageView.setFitHeight(10);
                imageView.setFitWidth(10);
                log("loadRankIcon() - Image loaded successfully from " + resourcePath);
                return imageView;
            } catch (Exception e) {
                log("loadRankIcon() - WARNING: Failed to load image: " + e.getMessage());
            }
        } else {
            log("loadRankIcon() - WARNING: Icon resource not found: " + resourcePath);
        }
        
        return null;
    }
    
    // ==================== Tile Initialization ====================
    
    private void initTiles() {
        log("=== initTiles() called ===");
        
        log("Creating Fluid tile...");
        initFluidTile();
        log("Fluid tile created");
        
        log("Creating LED tile...");
        initLEDTile();
        log("LED tile created");
        
        log("Creating Status tile...");
        initStatusTile();
        log("Status tile created");
        
        log("Creating Custom tile (TopX)...");
        initCustomTile();
        log("Custom tile created");
        
        log("=== initTiles() complete ===");
    }
    
    private void initFluidTile() {
        log("initFluidTile() - Building FLUID tile with size " + tileWidth + "x" + tileHeight);
        
        fluidTile = TileBuilder.create()
                .skinType(Tile.SkinType.FLUID)
                .prefSize(tileWidth, tileHeight)
                .title("Gravity last update: ")
                .text("ADS Blocked")
                .unit("\u0025")
                .decimals(0)
                .barColor(Tile.RED)
                .animated(true)
                .build();
        
        fluidTile.setValue(0);
        log("initFluidTile() - Fluid tile built, initial value set to 0");
    }
    
    private void initLEDTile() {
        log("initLEDTile() - Building LED tile with size " + tileWidth + "x" + tileHeight);
        
        ledTile = TileBuilder.create()
                .skinType(Tile.SkinType.LED)
                .prefSize(tileWidth, tileHeight)
                .title("Version: ")
                .description("Description")
                .text("Whatever text")
                .build();
        
        ledTile.setActive(true);
        log("initLEDTile() - LED tile built, active set to true");
    }
    
    private void initStatusTile() {
        log("initStatusTile() - Building STATUS tile with size " + tileWidth + "x" + tileHeight);
        
        Indicator leftGraphics = new Indicator(Tile.BLUE);
        leftGraphics.setOn(true);
        
        Indicator middleGraphics = new Indicator(Tile.RED);
        middleGraphics.setOn(true);
        
        Indicator rightGraphics = new Indicator(Tile.GREEN);
        rightGraphics.setOn(true);
        
        log("initStatusTile() - Indicators created (Blue, Red, Green)");
        
        statusTile = TileBuilder.create()
                .skinType(Tile.SkinType.STATUS)
                .prefSize(tileWidth, tileHeight)
                .title("Nbr of domains blocked: ")
                .description("")
                .leftText("Processed")
                .middleText("Blocked")
                .rightText("Accepted")
                .leftGraphics(leftGraphics)
                .middleGraphics(middleGraphics)
                .rightGraphics(rightGraphics)
                .text("Gravity")
                .build();
        
        statusTile.setLeftValue(0);
        statusTile.setMiddleValue(0);
        statusTile.setRightValue(0);
        
        log("initStatusTile() - Status tile built, initial values set to 0");
    }
    
    private void initCustomTile() {
        log("initCustomTile() - Building CUSTOM tile for Top " + topX + " Blocked");
        
        dataTable = new VBox();
        dataTable.setFillWidth(true);
        dataTable.setAlignment(Pos.CENTER_LEFT);
        log("initCustomTile() - DataTable VBox created");
        
        String copyright = "Copyright (C) " + Year.now().getValue() + ".  Reda ELFARISSI aka foxy999";
        
        topXTile = TileBuilder.create()
                .skinType(Tile.SkinType.CUSTOM)
                .prefSize(tileWidth, tileHeight)
                .title("Top " + topX + " Blocked")
                .text(copyright)
                .build();
        
        log("initCustomTile() - TopX tile built with title 'Top " + topX + " Blocked'");
    }
    
    // ==================== Context Menu ====================
    
    private void initializeContextMenu() {
        log("=== initializeContextMenu() called ===");
        
        MenuItem hideToTrayItem = new MenuItem("Hide to Tray");
        hideToTrayItem.setOnAction(_ -> {
            log("Context menu: 'Hide to Tray' clicked");
            WidgetApplication.hideToTray();
        });
        
        MenuItem refreshItem = new MenuItem("Refresh All Now");
        refreshItem.setOnAction(_ -> {
            log("Context menu: 'Refresh All Now' clicked");
            inflateAllData();
        });
        
        MenuItem configItem = new MenuItem("Settings");
        configItem.setOnAction(_ -> {
            log("Context menu: 'Settings' clicked");
            WidgetApplication.openConfigurationWindow();
        });
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(_ -> {
            log("Context menu: 'Exit' clicked - shutting down");
            shutdown();
            System.exit(0);
        });
        
        log("Menu items created: Hide to Tray, Refresh All Now, Settings, Exit");
        
        final ContextMenu contextMenu = new ContextMenu(hideToTrayItem, refreshItem, configItem, exitItem);
        
        // Attach context menu handler
        log("Attaching context menu to gridPane...");
        attachContextMenuHandler(gridPane, contextMenu);
        
        log("Attaching context menu to " + gridPane.getChildren().size() + " child nodes...");
        for (Node child : gridPane.getChildren()) {
            attachContextMenuHandler(child, contextMenu);
        }
        
        log("=== initializeContextMenu() complete ===");
    }
    
    private void attachContextMenuHandler(Node node, ContextMenu contextMenu) {
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isSecondaryButtonDown()) {
                log("Right-click detected, showing context menu");
                contextMenu.show(gridPane, event.getScreenX(), event.getScreenY());
            } else if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });
    }
    
    // ==================== Utility Methods ====================
    
    private String truncateDomain(String domain) {
        if (domain == null) {
            return "";
        }
        return domain.length() <= DOMAIN_TRUNCATE_LENGTH 
                ? domain 
                : domain.substring(0, DOMAIN_TRUNCATE_LENGTH) + TRUNCATION_SUFFIX;
    }
    
    private String formatConfig(PiholeConfig config) {
        return config != null 
                ? config.getIPAddress() + ":" + config.getPort() 
                : "null";
    }
    
    private String formatWidgetConfig(WidgetConfig config) {
        return config != null 
                ? "size=" + config.getSize() + ", layout=" + config.getLayout() 
                : "null";
    }
    
    // ==================== Public API ====================
    
    @FXML
    public void openConfigurationWindow() {
        log("openConfigurationWindow() called");
        WidgetApplication.openConfigurationWindow();
    }
    
    public FlowGridPane getGridPane() {
        log("getGridPane() called - returning gridPane: " + (gridPane != null ? "exists" : "null"));
        return gridPane;
    }
    
    public PiholeConfig getConfigDNS1() {
        log("getConfigDNS1() - returning: " + formatConfig(configDNS1));
        return configDNS1;
    }
    
    public void setConfigDNS1(PiholeConfig configDNS1) {
        log("setConfigDNS1() - setting to: " + formatConfig(configDNS1));
        this.configDNS1 = configDNS1;
    }
    
    public PiholeConfig getConfigDNS2() {
        log("getConfigDNS2() - returning: " + formatConfig(configDNS2));
        return configDNS2;
    }
    
    public void setConfigDNS2(PiholeConfig configDNS2) {
        log("setConfigDNS2() - setting to: " + formatConfig(configDNS2));
        this.configDNS2 = configDNS2;
    }
    
    public WidgetConfig getWidgetConfig() {
        log("getWidgetConfig() - returning: " + formatWidgetConfig(widgetConfig));
        return widgetConfig;
    }
    
    public void setWidgetConfig(WidgetConfig widgetConfig) {
        log("setWidgetConfig() - setting to: " + formatWidgetConfig(widgetConfig));
        this.widgetConfig = widgetConfig;
    }
}
