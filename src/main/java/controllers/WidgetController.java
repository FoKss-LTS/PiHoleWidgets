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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.configuration.DnsBlockerConfig;
import domain.configuration.WidgetConfig;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.addons.Indicator;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import helpers.HelperService;
import helpers.ThemeManager;
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
import javafx.scene.input.MouseButton;
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
import services.DnsBlockerHandler;
import services.DnsBlockerHandlerFactory;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Pi-hole Widget dashboard.
 * Manages the display and periodic refresh of Pi-hole statistics across
 * multiple tiles.
 */
public class WidgetController implements Initializable {

    // ==================== Constants ====================

    private static final String WIDGET_VERSION = loadVersion();
    private static final int DEFAULT_TOP_X = 5;
    private static final int DOMAIN_TRUNCATE_LENGTH = 20;
    private static final String TRUNCATION_SUFFIX = "..";
    private static final String RANK_ICON_PATH_PATTERN = "/media/images/%d.png";
    private static final int TOOLTIP_DELAY_MS = 200;
    private static final DateTimeFormatter STATS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Scheduler defaults in seconds (aligned with legacy behaviour)
    private static final long DEFAULT_STATUS_REFRESH_INTERVAL = WidgetConfig.DEFAULT_STATUS_UPDATE_SEC;
    private static final long DEFAULT_FLUID_REFRESH_INTERVAL = WidgetConfig.DEFAULT_FLUID_UPDATE_SEC;
    private static final long DEFAULT_ACTIVE_REFRESH_INTERVAL = WidgetConfig.DEFAULT_ACTIVE_UPDATE_SEC;
    private static final long DEFAULT_TOPX_REFRESH_INTERVAL = WidgetConfig.DEFAULT_TOPX_UPDATE_SEC;

    // Default tile dimensions
    private static final double DEFAULT_TILE_WIDTH = 200;
    private static final double DEFAULT_TILE_HEIGHT = 200;

    // ==================== Logging ====================

    private static String loadVersion() {
        try (var stream = WidgetController.class.getResourceAsStream("/version.properties")) {
            if (stream == null)
                return "Unknown";
            Properties props = new Properties();
            props.load(stream);
            return props.getProperty("version", "Unknown");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(WidgetController.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));
    private static final ObjectMapper JSON = new ObjectMapper();

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

    // DNS blocker handler (supports both Pi-hole and AdGuard Home)
    private DnsBlockerHandler dnsBlockerHandler;
    /*
     * DNS2 support intentionally disabled.
     * User request:
     * "no need for 2 DNS management, comment all code related to 2 DNSs"
     *
     * private DnsBlockerHandler dnsBlocker2;
     */

    // Configuration
    private DnsBlockerConfig configDNS1;
    /*
     * DNS2 support intentionally disabled.
     * private DnsBlockerConfig configDNS2;
     */
    private WidgetConfig widgetConfig;
    private long statusRefreshIntervalSec = DEFAULT_STATUS_REFRESH_INTERVAL;
    private long fluidRefreshIntervalSec = DEFAULT_FLUID_REFRESH_INTERVAL;
    private long activeRefreshIntervalSec = DEFAULT_ACTIVE_REFRESH_INTERVAL;
    private long topXRefreshIntervalSec = DEFAULT_TOPX_REFRESH_INTERVAL;

    // Centralized scheduler for all periodic tasks
    private ScheduledExecutorService scheduler;

    private enum BlockingState {
        ENABLED, DISABLED, MIXED, UNKNOWN
    }

    private volatile BlockingState blockingState = BlockingState.UNKNOWN;
    private volatile boolean fallbackToggleFlag = false;

    // ==================== FXML Injected Fields ====================

    @FXML
    private Pane rootPane;

    @FXML
    private Label dakLabel;

    // ==================== Constructor ====================

    public WidgetController(DnsBlockerConfig configDNS1, WidgetConfig widgetConfig) {
        log("=== WidgetController constructor called ===");
        log("ConfigDNS1: " + formatConfig(configDNS1));
        log("WidgetConfig: " + formatWidgetConfig(widgetConfig));

        this.configDNS1 = configDNS1;
        this.widgetConfig = widgetConfig;
    }

    // ==================== Initialization ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log("=== initialize() called ===");
        log("Location: " + location);
        log("ConfigDNS1 present: " + (configDNS1 != null));

        if (configDNS1 == null) {
            log("ERROR: DNS1 configuration is null!");
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

        log("Wiring DNS blocking toggle (LED circle click)...");
        setupDnsBlockingToggle();

        log("Starting schedulers...");
        applyIntervalsFromConfig();
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
        String copyright = "Copyright (C) " + Year.now().getValue() + ".  Reda ELFARISSI aka FoKss-LTS";
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

        // Apply theme-aware background color
        String theme = widgetConfig != null ? widgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;
        Color bgColor = ThemeManager.getBackgroundColor(theme);
        gridPane.setBackground(new Background(new BackgroundFill(bgColor, CornerRadii.EMPTY, Insets.EMPTY)));

        log("FlowGridPane created with theme: " + theme);
    }

    // ==================== Scheduler Management ====================

    private void applyIntervalsFromConfig() {
        if (widgetConfig == null) {
            statusRefreshIntervalSec = DEFAULT_STATUS_REFRESH_INTERVAL;
            fluidRefreshIntervalSec = DEFAULT_FLUID_REFRESH_INTERVAL;
            activeRefreshIntervalSec = DEFAULT_ACTIVE_REFRESH_INTERVAL;
            topXRefreshIntervalSec = DEFAULT_TOPX_REFRESH_INTERVAL;
            return;
        }

        statusRefreshIntervalSec = positiveOrDefault(widgetConfig.updateStatusSec(), DEFAULT_STATUS_REFRESH_INTERVAL);
        fluidRefreshIntervalSec = positiveOrDefault(widgetConfig.updateFluidSec(), DEFAULT_FLUID_REFRESH_INTERVAL);
        activeRefreshIntervalSec = positiveOrDefault(widgetConfig.updateActiveSec(), DEFAULT_ACTIVE_REFRESH_INTERVAL);
        topXRefreshIntervalSec = positiveOrDefault(widgetConfig.updateTopXSec(), DEFAULT_TOPX_REFRESH_INTERVAL);

        log("Scheduler intervals applied from config - status: " + statusRefreshIntervalSec + "s, active: "
                + activeRefreshIntervalSec
                + "s, fluid: " + fluidRefreshIntervalSec + "s, topX: " + topXRefreshIntervalSec + "s");
    }

    private long positiveOrDefault(int candidate, long defaultValue) {
        return candidate > 0 ? candidate : defaultValue;
    }

    private void initializeSchedulers() {
        log("Initializing centralized scheduler with virtual threads...");

        // Use virtual threads for lightweight scheduling (Java 21+ feature)
        scheduler = Executors.newScheduledThreadPool(4, Thread.ofVirtual()
                .name("pihole-widget-", 0)
                .factory());

        scheduler.scheduleAtFixedRate(this::inflateStatusData, 0, statusRefreshIntervalSec, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::inflateActiveData, 0, activeRefreshIntervalSec, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::inflateFluidData, 0, fluidRefreshIntervalSec, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::inflateTopXData, 0, topXRefreshIntervalSec, TimeUnit.SECONDS);

        log("All schedulers initialized - Status: " + statusRefreshIntervalSec + "s, " +
                "Active: " + activeRefreshIntervalSec + "s, " +
                "Fluid: " + fluidRefreshIntervalSec + "s, " +
                "TopX: " + topXRefreshIntervalSec + "s");
    }

    private void runAsync(Runnable task) {
        if (task == null)
            return;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.execute(task);
        } else {
            Thread.ofVirtual().name("pihole-widget-once-", 0).start(task);
        }
    }

    /**
     * Shuts down the scheduler gracefully. Should be called when the widget is
     * closed.
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
        scheduler = null;
    }

    private void restartSchedulers() {
        shutdown();
        initializeSchedulers();
        inflateAllData();
    }

    // ==================== Pi-hole Data Management ====================

    public void refreshPihole() {
        log("=== refreshPihole() called ===");

        if (configDNS1 != null) {
            log("Creating DNS blocker handler for: " + configDNS1.platform() + " at " +
                    configDNS1.getIPAddress() + ":" + configDNS1.getPort());
            dnsBlockerHandler = DnsBlockerHandlerFactory.createHandler(configDNS1);
            logInfo("DNS Blocker (" + configDNS1.platform() + ") version: " + dnsBlockerHandler.getVersion());
            log("DNS blocker handler created");
        } else {
            log("ConfigDNS1 is null, skipping DNS handler creation");
        }
        /*
         * DNS2 support intentionally disabled.
         * User request:
         * "no need for 2 DNS management, comment all code related to 2 DNSs"
         *
         * if (configDNS2 != null) {
         * log("Creating DNS blocker handler for DNS2: " + configDNS2.platform() +
         * " at " +
         * configDNS2.getIPAddress() + ":" + configDNS2.getPort());
         * dnsBlocker2 = DnsBlockerHandlerFactory.createHandler(configDNS2);
         * log("DNS blocker handler DNS2 created");
         * } else {
         * log("ConfigDNS2 is null, skipping DNS2 handler creation");
         * }
         */

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
     * Fetches Pi-hole stats from the single configured instance as a JSON string.
     * DNS2 support intentionally disabled.
     */
    private String fetchPiholeStatsJson() {
        return (dnsBlockerHandler != null) ? dnsBlockerHandler.getStats() : "";
    }

    // ==================== Data Inflation Methods ====================

    private static String formatStatsFetchedAt(Instant fetchedAt) {
        if (fetchedAt == null)
            return "Stats: unknown";
        String time = STATS_TIME_FORMATTER.format(
                fetchedAt.atZone(ZoneId.systemDefault())
                        .toLocalTime()
                        .truncatedTo(ChronoUnit.SECONDS));
        return time;
    }

    public void inflateStatusData() {
        log("=== inflateStatusData() called ===");
        runAsync(() -> {
            String statsJson = fetchPiholeStatsJson();
            SummaryStats s1 = parseSummaryStats(statsJson);
            CombinedStats combined = combineStats(s1, SummaryStats.inactive());

            String lastBlocked = "";
            if (dnsBlockerHandler != null) {
                lastBlocked = dnsBlockerHandler.getLastBlocked();
            }
            String finalLastBlocked = lastBlocked == null ? "" : lastBlocked;

            Platform.runLater(() -> {
                log("inflateStatusData - updating UI...");

                statusTile.setLeftValue(combined.totalQueries());
                statusTile.setMiddleValue(combined.blockedQueries());
                statusTile.setRightValue(combined.acceptedQueries());
                statusTile.setDescription(HelperService.getHumanReadablePriceFromNumber(combined.domainsBlocked()));
                statusTile.setText(finalLastBlocked);

                log("inflateStatusData complete");
            });
        });
    }

    public void inflateFluidData() {
        log("=== inflateFluidData() called ===");
        runAsync(() -> {
            String statsJson = fetchPiholeStatsJson();
            if (statsJson == null || statsJson.isBlank()) {
                log("WARNING: Pi-hole inactive, skipping fluid update");
                return;
            }
            final Instant fetchedAt = Instant.now();

            SummaryStats s1 = parseSummaryStats(statsJson);
            CombinedStats combined = combineStats(s1, SummaryStats.inactive());

            double adsPercentage = combined.percentBlocked();
            String statsFetchedText = formatStatsFetchedAt(fetchedAt);

            Platform.runLater(() -> {
                log("inflateFluidData - updating UI...");
                fluidTile.setValue(adsPercentage);
                fluidTile.setTitle("Widget Version: " + WIDGET_VERSION);
                fluidTile.setText("Stats fetched at " + statsFetchedText);
                log("inflateFluidData complete");
            });
        });
    }

    public void inflateActiveData() {
        log("=== inflateActiveData() called ===");
        runAsync(() -> {
            String statsJson = fetchPiholeStatsJson();
            SummaryStats s1 = parseSummaryStats(statsJson);

            Boolean b1 = (dnsBlockerHandler != null && statsJson != null && !statsJson.isBlank())
                    ? fetchDnsBlockingEnabled(dnsBlockerHandler)
                    : null;
            BlockingState state = computeBlockingStateSingle(b1, s1);
            this.blockingState = state;

            final String ipsText = (dnsBlockerHandler != null && configDNS1 != null)
                    ? configDNS1.getIPAddress()
                    : "";

            String apiVersion = "";
            if (dnsBlockerHandler != null)
                apiVersion = dnsBlockerHandler.getVersion();
            String finalApiVersion = apiVersion == null ? "" : apiVersion;

            String gravityUpdate = "";
            if (dnsBlockerHandler != null)
                gravityUpdate = dnsBlockerHandler.getGravityLastUpdate();
            String finalGravityUpdate = gravityUpdate == null ? "" : gravityUpdate;

            Platform.runLater(() -> {
                log("inflateActiveData - updating UI...");

                var apiTitle = finalApiVersion.isBlank()
                        ? "API Version: N/A"
                        : "API Version: " + finalApiVersion;
                var gravityLabel = finalGravityUpdate.isBlank()
                        ? "Gravity Last Update: N/A"
                        : finalGravityUpdate;

                ledTile.setTitle(apiTitle);
                ledTile.setDescription((statsJson == null || statsJson.isBlank()) ? "No active Pi-hole" : ipsText);

                if (statsJson == null || statsJson.isBlank()) {
                    ledTile.setActiveColor(Color.RED);
                    ledTile.setActive(false);
                    ledTile.setText(gravityLabel);
                    ledTile.setTooltipText("No active Pi-hole");
                    return;
                }

                // Color reflects DNS blocking status (the LED “circle”)
                switch (state) {
                    case ENABLED -> {
                        ledTile.setActiveColor(Color.LIGHTGREEN);
                        ledTile.setActive(true);
                        ledTile.setText(gravityLabel);
                        ledTile.setTooltipText("DNS blocking is ENABLED (click LED circle to disable)");
                    }
                    case DISABLED -> {
                        ledTile.setActiveColor(Color.RED);
                        ledTile.setActive(false);
                        ledTile.setText(gravityLabel);
                        ledTile.setTooltipText("DNS blocking is DISABLED (click LED circle to enable)");
                    }
                    case MIXED -> {
                        // DNS2 support intentionally disabled: MIXED state cannot happen with a single
                        // instance.
                        ledTile.setActiveColor(Color.ORANGE);
                        ledTile.setActive(true);
                        ledTile.setText(gravityLabel);
                        ledTile.setTooltipText("Click LED circle to toggle DNS blocking");
                    }
                    case UNKNOWN -> {
                        ledTile.setActiveColor(Color.LIGHTGREEN);
                        ledTile.setActive(true);
                        ledTile.setText(gravityLabel);
                        ledTile.setTooltipText("Click LED circle to toggle DNS blocking");
                    }
                }

                log("inflateActiveData complete");
            });
        });
    }

    public void inflateTopXData() {
        System.out.println("===== inflateTopXData() called from WidgetController =====");
        log("=== inflateTopXData() called ===");
        runAsync(() -> {
            if (dnsBlockerHandler == null) {
                log("WARNING: Pi-hole inactive, skipping topX update");
                return;
            }

            DnsBlockerHandler handler = dnsBlockerHandler;

            log("Fetching top " + topX + " blocked domains...");
            String topBlockedJson = handler.getTopXBlocked(topX);
            List<TopDomain> domains = parseTopBlockedDomains(topBlockedJson);

            Platform.runLater(() -> {
                log("inflateTopXData - updating UI...");

                HBox header = createTopXHeader();
                HBox spacerRow = createSpacerRow();

                List<Node> rows = new ArrayList<>();
                rows.add(header);
                rows.add(spacerRow);

                int rank = 1;
                for (TopDomain d : domains) {
                    String full = d.domain();
                    String truncated = truncateDomain(full);
                    String countText = HelperService.getHumanReadablePriceFromNumber(d.count());
                    rows.add(createTopBlockedItem(rank, full, truncated, countText));
                    rank++;
                }

                dataTable.getChildren().setAll(rows);
                topXTile.setGraphic(dataTable);

                log("inflateTopXData complete");
            });
        });
    }

    // ==================== UI Component Creation ====================

    private HBox createTopXHeader() {
        String theme = widgetConfig != null ? widgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;
        Color textColor = ThemeManager.getTextColor(theme);

        Label nameLabel = new Label("Domain");
        nameLabel.setTextFill(textColor);
        nameLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.NEVER);

        Label blocksLabel = new Label("Nbr Blocks");
        blocksLabel.setTextFill(textColor);
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
        log("createTopBlockedItem() - Creating item #" + rank + ": " + truncatedDomain + " (" + blockCount
                + " blocks)");

        String theme = widgetConfig != null ? widgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;
        Color textColor = ThemeManager.getTextColor(theme);

        // Try to load rank icon from classpath resources
        ImageView iconView = loadRankIcon(rank);

        Label domainLabel = new Label(truncatedDomain);
        domainLabel.setTextFill(textColor);
        domainLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(domainLabel, Priority.NEVER);

        Region spacer = new Region();
        spacer.setPrefSize(5, 5);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(blockCount);
        valueLabel.setTextFill(textColor);
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
     * 
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

        String theme = widgetConfig != null ? widgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;

        fluidTile = TileBuilder.create()
                .skinType(Tile.SkinType.FLUID)
                .prefSize(tileWidth, tileHeight)
                .title("Widget Version")
                .text("Widget Version: " + WIDGET_VERSION)
                .unit("\u0025")
                .decimals(0)
                .barColor(Tile.RED)
                .animated(true)
                .backgroundColor(ThemeManager.getTileBackgroundColor(theme))
                .foregroundColor(ThemeManager.getForegroundColor(theme))
                .titleColor(ThemeManager.getTitleColor(theme))
                .textColor(ThemeManager.getTextColor(theme))
                .valueColor(ThemeManager.getValueColor(theme))
                .unitColor(ThemeManager.getTextColor(theme))
                .build();

        fluidTile.setValue(0);
        log("initFluidTile() - Fluid tile built, initial value set to 0");
    }

    private void initLEDTile() {
        log("initLEDTile() - Building LED tile with size " + tileWidth + "x" + tileHeight);

        String theme = widgetConfig != null ? widgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;

        ledTile = TileBuilder.create()
                .skinType(Tile.SkinType.LED)
                .prefSize(tileWidth, tileHeight)
                .title("API Version")
                .description("Description")
                .text("Whatever text")
                .backgroundColor(ThemeManager.getTileBackgroundColor(theme))
                .foregroundColor(ThemeManager.getForegroundColor(theme))
                .titleColor(ThemeManager.getTitleColor(theme))
                .textColor(ThemeManager.getTextColor(theme))
                .descriptionColor(ThemeManager.getTextColor(theme))
                .build();

        ledTile.setActive(true);
        log("initLEDTile() - LED tile built, active set to true");
    }

    private void setupDnsBlockingToggle() {
        if (ledTile == null)
            return;

        ledTile.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY)
                return;
            if (!wasLedCircleClicked(event))
                return;
            toggleDnsBlocking();
            event.consume();
        });

        // Hint for users
        if (ledTile.getTooltipText() == null || ledTile.getTooltipText().isBlank()) {
            ledTile.setTooltipText("Click LED circle to toggle DNS blocking");
        }
    }

    /**
     * Best-effort detection: only toggle when the click originated on the LED
     * “circle”.
     * Falls back to allowing clicks on the tile if we can't identify the underlying
     * node.
     */
    private boolean wasLedCircleClicked(MouseEvent event) {
        if (event == null || event.getPickResult() == null)
            return true;
        Node n = event.getPickResult().getIntersectedNode();
        if (n == null)
            return true;
        while (n != null) {
            List<String> classes = n.getStyleClass();
            if (classes != null) {
                // TilesFX LED skin typically uses style classes like "led"
                if (classes.contains("led") || classes.contains("led-frame") || classes.contains("indicator")) {
                    return true;
                }
            }
            n = n.getParent();
        }
        // If we couldn't confirm (CSS classes vary between TilesFX versions),
        // allow toggling when the user clicks the LED tile.
        return true;
    }

    private void toggleDnsBlocking() {
        log("toggleDnsBlocking() called");
        runAsync(() -> {
            // Determine target state based on last-known state; if uncertain, re-check via
            // summary JSON.
            Boolean currentEnabled = switch (blockingState) {
                case ENABLED -> true;
                case DISABLED -> false;
                case MIXED, UNKNOWN -> null;
            };

            if (currentEnabled == null) {
                // Best-effort refresh from status endpoint (preferred) then summary fallback
                if (dnsBlockerHandler != null)
                    currentEnabled = fetchDnsBlockingEnabled(dnsBlockerHandler);
            }

            // Toggle: if still unknown, alternate locally so clicks still toggle
            boolean targetEnable;
            if (currentEnabled != null) {
                targetEnable = !currentEnabled;
            } else {
                fallbackToggleFlag = !fallbackToggleFlag;
                targetEnable = fallbackToggleFlag;
            }

            log("Toggling DNS blocking -> " + (targetEnable ? "ENABLED" : "DISABLED"));

            // Apply to all configured instances (best effort)
            if (dnsBlockerHandler != null)
                dnsBlockerHandler.setDnsBlocking(targetEnable, null);
            // DNS2 support intentionally disabled.
            // if (dnsBlocker2 != null) dnsBlocker2.setDnsBlocking(targetEnable, null);

            // Refresh LED/status immediately after change
            inflateActiveData();
            inflateStatusData();
        });
    }

    // ==================== JSON Parsing ====================

    private record SummaryStats(boolean active, long totalQueries, long blockedQueries, double percentBlocked,
            long domainsBlocked, Boolean dnsBlockingEnabled) {
        static SummaryStats inactive() {
            return new SummaryStats(false, 0L, 0L, 0.0, 0L, null);
        }
    }

    private record CombinedStats(long totalQueries, long blockedQueries, long acceptedQueries, double percentBlocked,
            long domainsBlocked) {
    }

    private record TopDomain(String domain, long count) {
    }

    private SummaryStats parseSummaryStats(String json) {
        if (json == null || json.isBlank())
            return SummaryStats.inactive();
        try {
            JsonNode root = JSON.readTree(json);

            long total = firstLong(root,
                    path("queries", "total"),
                    path("queries", "total_queries"),
                    path("dns_queries_today"));

            long blocked = firstLong(root,
                    path("queries", "blocked"),
                    path("queries", "blocked_queries"),
                    path("ads_blocked_today"));

            double percent = firstDouble(root,
                    path("queries", "percent_blocked"),
                    path("ads_percentage_today"));
            if ((percent <= 0.0) && total > 0L && blocked >= 0L) {
                percent = (blocked / (double) total) * 100.0;
            }

            long domainsBlocked = firstLong(root,
                    path("domains", "blocked"),
                    path("domains_being_blocked"),
                    path("gravity", "domains_being_blocked"));

            Boolean dnsEnabled = parseDnsBlockingEnabled(root);

            return new SummaryStats(true, total, blocked, percent, domainsBlocked, dnsEnabled);
        } catch (Exception e) {
            log("WARNING: Failed to parse summary stats JSON: " + e.getMessage());
            return SummaryStats.inactive();
        }
    }

    private CombinedStats combineStats(SummaryStats s1, SummaryStats s2) {
        long total = 0L;
        long blocked = 0L;
        long domainsBlocked = 0L;

        if (s1 != null && s1.active()) {
            total += s1.totalQueries();
            blocked += s1.blockedQueries();
            domainsBlocked = Math.max(domainsBlocked, s1.domainsBlocked());
        }
        if (s2 != null && s2.active()) {
            total += s2.totalQueries();
            blocked += s2.blockedQueries();
            domainsBlocked = Math.max(domainsBlocked, s2.domainsBlocked());
        }

        long accepted = Math.max(0L, total - blocked);
        double percent = (total > 0L) ? (blocked / (double) total) * 100.0 : 0.0;

        return new CombinedStats(total, blocked, accepted, percent, domainsBlocked);
    }

    /*
     * DNS2 support intentionally disabled.
     * User request:
     * "no need for 2 DNS management, comment all code related to 2 DNSs"
     *
     * private BlockingState computeBlockingState(PiholeStats stats, Boolean b1,
     * Boolean b2, SummaryStats s1, SummaryStats s2) {
     * if (stats == null || stats.bothInactive()) return BlockingState.UNKNOWN;
     * // If status endpoint failed, fall back to any info we might have found in
     * summary
     * if (b1 == null && stats.isActive1() && s1 != null) b1 =
     * s1.dnsBlockingEnabled();
     * if (b2 == null && stats.isActive2() && s2 != null) b2 =
     * s2.dnsBlockingEnabled();
     *
     * if (b1 == null && b2 == null) return BlockingState.UNKNOWN;
     * if (b1 != null && b2 == null) return b1 ? BlockingState.ENABLED :
     * BlockingState.DISABLED;
     * if (b1 == null) return b2 ? BlockingState.ENABLED : BlockingState.DISABLED;
     * if (b1.equals(b2)) return b1 ? BlockingState.ENABLED :
     * BlockingState.DISABLED;
     * return BlockingState.MIXED;
     * }
     */

    private BlockingState computeBlockingStateSingle(Boolean enabled, SummaryStats summary) {
        // If status endpoint failed, fall back to any info we might have found in
        // summary JSON.
        if (enabled == null && summary != null)
            enabled = summary.dnsBlockingEnabled();
        if (enabled == null)
            return BlockingState.UNKNOWN;
        return enabled ? BlockingState.ENABLED : BlockingState.DISABLED;
    }

    private Boolean fetchDnsBlockingEnabled(DnsBlockerHandler handler) {
        if (handler == null)
            return null;

        // Preferred: dedicated endpoint
        String statusJson = handler.getDnsBlockingStatus();
        Boolean enabled = parseDnsBlockingEnabledSafe(statusJson);
        if (enabled != null)
            return enabled;

        // Fallback: summary (some versions include status info there)
        String summaryJson = handler.getStats();
        return parseDnsBlockingEnabledSafe(summaryJson);
    }

    private Boolean parseDnsBlockingEnabledSafe(String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            JsonNode root = JSON.readTree(json);
            return parseDnsBlockingEnabled(root);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean parseDnsBlockingEnabled(JsonNode root) {
        if (root == null)
            return null;

        // Common patterns across Pi-hole APIs:
        // - status: "enabled" / "disabled"
        // - blocking: "enabled" / "disabled"
        // - blocking: true/false
        JsonNode statusNode = firstNode(root,
                path("status"),
                path("blocking"),
                path("dns", "blocking"),
                path("dns", "status"));

        if (statusNode == null || statusNode.isMissingNode() || statusNode.isNull())
            return null;

        if (statusNode.isBoolean()) {
            return statusNode.asBoolean();
        }

        String txt = statusNode.asText("");
        if (txt.equalsIgnoreCase("enabled"))
            return true;
        if (txt.equalsIgnoreCase("disabled"))
            return false;
        if (txt.equalsIgnoreCase("true"))
            return true;
        if (txt.equalsIgnoreCase("false"))
            return false;

        return null;
    }

    private List<TopDomain> parseTopBlockedDomains(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            JsonNode root = JSON.readTree(json);
            // Pi-hole format: {"top_ads": {"domain1.com": 123, "domain2.com": 456}}
            JsonNode topAds = root.path("top_ads");
            if (!topAds.isObject())
                return List.of();

            List<TopDomain> result = new ArrayList<>();
            var fields = topAds.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String domain = entry.getKey();
                long count = entry.getValue().asLong(0L);
                if (domain == null || domain.isBlank())
                    continue;
                result.add(new TopDomain(domain, Math.max(0L, count)));
            }
            return result;
        } catch (Exception e) {
            System.out.println("WARNING: Failed to parse top domains JSON: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    private static String[] path(String... parts) {
        return parts;
    }

    private static JsonNode nodeAt(JsonNode root, String[] path) {
        JsonNode n = root;
        for (String p : path) {
            if (n == null)
                return null;
            n = n.path(p);
        }
        return n;
    }

    private static JsonNode firstNode(JsonNode root, String[]... paths) {
        if (root == null || paths == null)
            return null;
        for (String[] p : paths) {
            JsonNode n = nodeAt(root, p);
            if (n != null && !n.isMissingNode() && !n.isNull())
                return n;
        }
        return null;
    }

    private static long firstLong(JsonNode root, String[]... paths) {
        JsonNode n = firstNode(root, paths);
        if (n == null)
            return 0L;
        if (n.isNumber())
            return n.asLong(0L);
        String txt = n.asText("");
        try {
            return Long.parseLong(txt);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static double firstDouble(JsonNode root, String[]... paths) {
        JsonNode n = firstNode(root, paths);
        if (n == null)
            return 0.0;
        if (n.isNumber())
            return n.asDouble(0.0);
        String txt = n.asText("");
        try {
            return Double.parseDouble(txt);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private void initStatusTile() {
        log("initStatusTile() - Building STATUS tile with size " + tileWidth + "x" + tileHeight);

        String theme = widgetConfig != null ? widgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;

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
                .title("Lists ")
                .leftText("Processed")
                .middleText("Blocked")
                .rightText("Accepted")
                .leftGraphics(leftGraphics)
                .middleGraphics(middleGraphics)
                .rightGraphics(rightGraphics)
                .text("Gravity Status")
                .backgroundColor(ThemeManager.getTileBackgroundColor(theme))
                .foregroundColor(ThemeManager.getForegroundColor(theme))
                .titleColor(ThemeManager.getTitleColor(theme))
                .textColor(ThemeManager.getTextColor(theme))
                .valueColor(ThemeManager.getValueColor(theme))
                .build();

        statusTile.setLeftValue(0);
        statusTile.setMiddleValue(0);
        statusTile.setRightValue(0);

        log("initStatusTile() - Status tile built, initial values set to 0");
    }

    private void initCustomTile() {
        log("initCustomTile() - Building CUSTOM tile for Top " + topX + " Blocked");

        String theme = widgetConfig != null ? widgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;

        dataTable = new VBox();
        dataTable.setFillWidth(true);
        dataTable.setAlignment(Pos.CENTER_LEFT);
        log("initCustomTile() - DataTable VBox created");

        String copyright = "Copyright (C) " + Year.now().getValue() + ".  Reda ELFARISSI aka FoKss-LTS";

        topXTile = TileBuilder.create()
                .skinType(Tile.SkinType.CUSTOM)
                .prefSize(tileWidth, tileHeight)
                .title("Top " + topX + " Blocked")
                .text(copyright)
                .backgroundColor(ThemeManager.getTileBackgroundColor(theme))
                .foregroundColor(ThemeManager.getForegroundColor(theme))
                .titleColor(ThemeManager.getTitleColor(theme))
                .textColor(ThemeManager.getTextColor(theme))
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

    private String formatConfig(DnsBlockerConfig config) {
        return config != null
                ? config.getIPAddress() + ":" + config.getPort() + " (" + config.platform() + ")"
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

    public Pane getGridPane() {
        log("getGridPane() called - returning gridPane: " + (gridPane != null ? "exists" : "null"));
        return gridPane;
    }

    public DnsBlockerConfig getConfigDNS1() {
        log("getConfigDNS1() - returning: " + formatConfig(configDNS1));
        return configDNS1;
    }

    public void setConfigDNS1(DnsBlockerConfig configDNS1) {
        log("setConfigDNS1() - setting to: " + formatConfig(configDNS1));
        this.configDNS1 = configDNS1;
    }

    /*
     * DNS2 support intentionally disabled.
     *
     * public PiholeConfig getConfigDNS2() {
     * log("getConfigDNS2() - returning: " + formatConfig(configDNS2));
     * return configDNS2;
     * }
     *
     * public void setConfigDNS2(PiholeConfig configDNS2) {
     * log("setConfigDNS2() - setting to: " + formatConfig(configDNS2));
     * this.configDNS2 = configDNS2;
     * }
     */

    public WidgetConfig getWidgetConfig() {
        log("getWidgetConfig() - returning: " + formatWidgetConfig(widgetConfig));
        return widgetConfig;
    }

    public void setWidgetConfig(WidgetConfig widgetConfig) {
        log("setWidgetConfig() - setting to: " + formatWidgetConfig(widgetConfig));
        this.widgetConfig = widgetConfig;
        applyIntervalsFromConfig();
        if (scheduler != null) {
            restartSchedulers();
        }
    }

    /**
     * Applies new DNS and widget configuration values and refreshes the UI.
     */
    public void applyConfiguration(DnsBlockerConfig newConfigDNS1, WidgetConfig newWidgetConfig) {
        setConfigDNS1(newConfigDNS1);
        setWidgetConfig(newWidgetConfig);
        refreshPihole();
        String theme = newWidgetConfig != null ? newWidgetConfig.getTheme() : ThemeManager.DEFAULT_THEME;
        applyTheme(theme);
    }

    /**
     * Triggers a full data refresh across all tiles.
     * Invoked when the widget becomes visible again (e.g., restored from tray).
     */
    public void refreshAllTiles() {
        log("refreshAllTiles() called");
        inflateAllData();
    }

    /**
     * Applies the specified theme to the widget.
     * Updates background colors and other theme-specific styling.
     *
     * @param theme the theme name (Dark or Light)
     */
    public void applyTheme(String theme) {
        log("applyTheme() - applying theme: " + theme);

        if (gridPane != null) {
            Color bgColor = ThemeManager.getBackgroundColor(theme);
            gridPane.setBackground(new Background(new BackgroundFill(bgColor, CornerRadii.EMPTY, Insets.EMPTY)));
            log("Grid pane background updated for theme: " + theme);
        }

        // Update copyright label color
        if (dakLabel != null) {
            Color textColor = ThemeManager.getMutedTextColor(theme);
            dakLabel.setTextFill(textColor);
        }

        // Update tile colors
        Color tileBg = ThemeManager.getTileBackgroundColor(theme);
        Color tileFg = ThemeManager.getForegroundColor(theme);
        Color titleColor = ThemeManager.getTitleColor(theme);
        Color textColor = ThemeManager.getTextColor(theme);
        Color valueColor = ThemeManager.getValueColor(theme);

        if (fluidTile != null) {
            fluidTile.setBackgroundColor(tileBg);
            fluidTile.setForegroundColor(tileFg);
            fluidTile.setTitleColor(titleColor);
            fluidTile.setTextColor(textColor);
            fluidTile.setValueColor(valueColor);
            fluidTile.setUnitColor(textColor);
        }

        if (ledTile != null) {
            ledTile.setBackgroundColor(tileBg);
            ledTile.setForegroundColor(tileFg);
            ledTile.setTitleColor(titleColor);
            ledTile.setTextColor(textColor);
            ledTile.setDescriptionColor(textColor);
        }

        if (statusTile != null) {
            statusTile.setBackgroundColor(tileBg);
            statusTile.setForegroundColor(tileFg);
            statusTile.setTitleColor(titleColor);
            statusTile.setTextColor(textColor);
            statusTile.setValueColor(valueColor);
        }

        if (topXTile != null) {
            topXTile.setBackgroundColor(tileBg);
            topXTile.setForegroundColor(tileFg);
            topXTile.setTitleColor(titleColor);
            topXTile.setTextColor(textColor);
        }

        // Update dataTable text colors for TopX tile
        if (dataTable != null) {
            updateDataTableTheme(theme);
        }

        log("Tile colors updated for theme: " + theme);
    }

    /**
     * Updates the TopX data table labels with theme-appropriate colors.
     */
    private void updateDataTableTheme(String theme) {
        Color textColor = ThemeManager.getTextColor(theme);
        for (Node node : dataTable.getChildren()) {
            if (node instanceof HBox row) {
                for (Node child : row.getChildren()) {
                    if (child instanceof Label label) {
                        label.setTextFill(textColor);
                    }
                }
            }
        }
    }
}
