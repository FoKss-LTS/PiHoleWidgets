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
import domain.pihole.PiHole;
import domain.pihole.TopAd;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.addons.Indicator;
import eu.hansolo.tilesfx.skins.LeaderBoardItem;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import helpers.HelperService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.util.Duration;
import services.pihole.PiHoleHandler;

import java.io.FileInputStream;
import java.net.URL;
import java.time.Year;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WidgetController implements Initializable {

    private double TILE_WIDTH = 200;
    private double TILE_HEIGHT = 200;
    private int cols = 2;
    private int rows = 2;

    private final String widgetVersion = "1.5.2";// + "_BETA";
    private Tile statusTile;
    private Tile ledTile;
    private Tile fluidTile;
    private Tile leaderBoardTile;
    private Tile topXTile;
    VBox dataTable;

    private PiHoleHandler piholeDns1;
    private PiHoleHandler piholeDns2;

    private PiholeConfig configDNS1 = null;
    private PiholeConfig configDNS2 = null;
    private WidgetConfig widgetConfig = null;
    private int topX;
    
    // Enable verbose logging via system property: -Dpihole.verbose=true
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));
    
    private static void log(String message) {
        if (VERBOSE) {
            System.out.println("[Widget] " + java.time.LocalDateTime.now() + " - " + message);
        }
    }


    @FXML
    private Pane rootPane;

    private FlowGridPane gridPane;

    @FXML
    private Label dakLabel;

    @FXML
    public void openConfigurationWindow() {
        log("openConfigurationWindow() called");
        WidgetApplication.openConfigurationWindow();
    }

    public WidgetController(PiholeConfig configDNS1, PiholeConfig configDNS2, WidgetConfig widgetConfig) {
        log("=== WidgetController constructor called ===");
        log("ConfigDNS1: " + (configDNS1 != null ? configDNS1.getIPAddress() + ":" + configDNS1.getPort() : "null"));
        log("ConfigDNS2: " + (configDNS2 != null ? configDNS2.getIPAddress() + ":" + configDNS2.getPort() : "null"));
        log("WidgetConfig: " + (widgetConfig != null ? "size=" + widgetConfig.getSize() + ", layout=" + widgetConfig.getLayout() : "null"));
        
        this.configDNS1 = configDNS1;
        this.configDNS2 = configDNS2;
        this.widgetConfig = widgetConfig;
    }

    public void initialize(URL location, ResourceBundle resources) {
        log("=== initialize() called ===");
        log("Location: " + location);
        log("ConfigDNS1 present: " + (configDNS1 != null));
        log("ConfigDNS2 present: " + (configDNS2 != null));

        if (configDNS1 != null || configDNS2 != null) {
            log("At least one config is present, proceeding with initialization...");

            topX = 5;
            log("TopX set to: " + topX);

            if (widgetConfig != null) {
                log("Widget config size: " + widgetConfig.getSize());
                log("Widget config layout: " + widgetConfig.getLayout());
                
                switch (widgetConfig.getSize()) {
                    case "Small":
                        log("Setting tile size: Small (150x150)");
                        TILE_WIDTH = 150;
                        TILE_HEIGHT = 150;
                        break;
                    case "Medium":
                        log("Setting tile size: Medium (200x200)");
                        TILE_WIDTH = 200;
                        TILE_HEIGHT = 200;
                        break;
                    case "Large":
                        log("Setting tile size: Large (350x350)");
                        TILE_WIDTH = 350;
                        TILE_HEIGHT = 350;
                        break;
                    case "XXL":
                        log("Setting tile size: XXL (500x500)");
                        TILE_WIDTH = 500;
                        TILE_HEIGHT = 500;
                        break;
                    case "Full Screen":
                        TILE_WIDTH = Screen.getPrimary().getBounds().getMaxX() / 4;
                        TILE_HEIGHT = Screen.getPrimary().getBounds().getMaxX() / 4;
                        log("Setting tile size: Full Screen (" + TILE_WIDTH + "x" + TILE_HEIGHT + ")");
                        break;
                    default:
                        log("Setting tile size: Default (200x200)");
                        TILE_WIDTH = 200;
                        TILE_HEIGHT = 200;
                }

                switch (widgetConfig.getLayout()) {
                    case "Horizontal":
                        cols = 4;
                        rows = 1;
                        log("Setting layout: Horizontal (4x1)");
                        break;
               /* case "Vertical":
                    cols = 1;
                    rows = 4;
                    break;*/
                    case "Square":
                        cols = 2;
                        rows = 2;
                        log("Setting layout: Square (2x2)");
                        break;
                    default:
                        cols = 2;
                        rows = 2;
                        log("Setting layout: Default (2x2)");
                }
            } else {
                log("Widget config is null, using defaults");
            }

            log("Calling refreshPihole()...");
            refreshPihole();

            log("Calling initTiles()...");
            initTiles();

            log("Starting schedulers...");
            initializeStatusScheduler();
            initializeActiveTileScheduler();
            initializeFluidTileScheduler();
            initializeTopXBlockedScheduler();
            log("All schedulers started");


            //rootPane.setStyle("-fx-background-color: rgba(42, 42, 42, 1);");

            dakLabel.setText("Copyright (C) " + Year.now().getValue() + ".  Reda ELFARISSI aka foxy999");
            dakLabel.setLayoutX(TILE_WIDTH + 1);
            dakLabel.setLayoutY((TILE_HEIGHT * 2) - 15);
            dakLabel.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.isPrimaryButtonDown()) {
                    openConfigurationWindow();
                }
            });

            //fluidTile.setBackgroundColor(new Color(42, 42, 42));

            log("Creating FlowGridPane with " + cols + " cols x " + rows + " rows");
            log("Adding tiles: ledTile, fluidTile, statusTile, topXTile");
            gridPane = new FlowGridPane(cols, rows, ledTile, fluidTile, statusTile, topXTile);
            gridPane.setHgap(5);
            gridPane.setVgap(5);
            gridPane.setAlignment(Pos.CENTER);
            gridPane.setCenterShape(true);
            gridPane.setPadding(new Insets(5));
            //gridPane.setPrefSize(TILE_WIDTH*2, 600);
            gridPane.setBackground(new Background(new BackgroundFill(Color.web("#101214"), CornerRadii.EMPTY, Insets.EMPTY)));
            log("FlowGridPane created with background color #101214");

            /*
            rootPane.getChildren().add(gridPane);
            rootPane.setPrefSize(TILE_WIDTH * 2, TILE_HEIGHT * 2);
            rootPane.getChildren().add(fluidTile);
            rootPane.getChildren().add(ledTile);
            rootPane.getChildren().add(statusTile);
            */
            log("Initializing context menu...");
            initializeContextMenu();
            log("=== Widget initialization complete ===");
        } else {
            log("ERROR: Both configurations are null!");
            System.out.println("configurations are empty");
        }

    }

    public void refreshPihole() {
        log("=== refreshPihole() called ===");
        
        if (configDNS1 != null) {
            log("Creating PiHoleHandler for DNS1: " + configDNS1.getIPAddress() + ":" + configDNS1.getPort());
            piholeDns1 = new PiHoleHandler(configDNS1.getIPAddress(), configDNS1.getPort(), configDNS1.getScheme(), configDNS1.getAUTH());
            log("PiHoleHandler DNS1 created");
        } else {
            log("ConfigDNS1 is null, skipping DNS1 handler creation");
        }

        if (configDNS2 != null) {
            log("Creating PiHoleHandler for DNS2: " + configDNS2.getIPAddress() + ":" + configDNS2.getPort());
            piholeDns2 = new PiHoleHandler(configDNS2.getIPAddress(), configDNS2.getPort(), configDNS2.getScheme(), configDNS2.getAUTH());
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

    public FlowGridPane getGridPane() {
        log("getGridPane() called - returning gridPane: " + (gridPane != null ? "exists" : "null"));
        return gridPane;
    }

    private void initializeStatusScheduler() {
        log("Initializing Status scheduler (every 5 seconds)...");
        ScheduledExecutorService executorStatusService = Executors.newSingleThreadScheduledExecutor();
        executorStatusService.scheduleAtFixedRate(this::inflateStatusData, 0, 5, TimeUnit.SECONDS);
        log("Status scheduler started");
    }

    private void initializeFluidTileScheduler() {
        log("Initializing Fluid tile scheduler (every 15 seconds)...");
        ScheduledExecutorService executorFluidService = Executors.newSingleThreadScheduledExecutor();
        executorFluidService.scheduleAtFixedRate(this::inflateFluidData, 0, 15, TimeUnit.SECONDS);
        log("Fluid tile scheduler started");
    }

    private void initializeActiveTileScheduler() {
        log("Initializing Active tile scheduler (every 60 seconds)...");
        ScheduledExecutorService executorActiveService = Executors.newSingleThreadScheduledExecutor();
        executorActiveService.scheduleAtFixedRate(this::inflateActiveData, 0, 60, TimeUnit.SECONDS);
        log("Active tile scheduler started");
    }

    private void initializeTopXBlockedScheduler() {
        log("Initializing TopX Blocked scheduler (every 5 seconds)...");
        ScheduledExecutorService executorLeaderBoardService = Executors.newSingleThreadScheduledExecutor();
        executorLeaderBoardService.scheduleAtFixedRate(this::inflateTopXData, 0, 5, TimeUnit.SECONDS);
        log("TopX Blocked scheduler started");
    }

    public void inflateStatusData() {
        log("=== inflateStatusData() called ===");
        Platform.runLater(() -> {
            log("inflateStatusData - runLater executing...");

            Long queries = 0L;
            Long blockedAds = 0L;
            Long queriesProcessed = 0L;
            Long domainsBlocked = 0L;


            PiHole pihole1 = null;

            if (piholeDns1 != null) {
                log("Fetching stats from DNS1...");
                pihole1 = piholeDns1.getPiHoleStats();
                log("DNS1 stats: " + (pihole1 != null ? "received" : "null"));
            }

            PiHole pihole2 = null;
            if (piholeDns2 != null) {
                log("Fetching stats from DNS2...");
                pihole2 = piholeDns2.getPiHoleStats();
                log("DNS2 stats: " + (pihole2 != null ? "received" : "null"));
            }


            if (pihole1 != null) {
                queries += pihole1.getDns_queries_today();
                blockedAds += pihole1.getAds_blocked_today();
                queriesProcessed += pihole1.getQueries_forwarded();
                queriesProcessed += pihole1.getQueries_cached();
                domainsBlocked = pihole1.getDomains_being_blocked();
                log("DNS1 data - Queries: " + queries + ", Blocked: " + blockedAds + ", Processed: " + queriesProcessed);
            }
            if (pihole2 != null) {
                queries += pihole2.getDns_queries_today();
                blockedAds += pihole2.getAds_blocked_today();
                queriesProcessed += pihole2.getQueries_forwarded();
                queriesProcessed += pihole2.getQueries_cached();
                log("DNS2 data added - Total Queries: " + queries + ", Total Blocked: " + blockedAds);
            }

            log("Updating status tile with values - Left: " + queries + ", Middle: " + blockedAds + ", Right: " + queriesProcessed);
            statusTile.setLeftValue(queries);
            statusTile.setMiddleValue(blockedAds);
            statusTile.setRightValue(queriesProcessed);

            statusTile.setDescription(HelperService.getHumanReadablePriceFromNumber(domainsBlocked));

            String lastBlocked = piholeDns1.getLastBlocked();
            log("Last blocked: " + lastBlocked);
            statusTile.setText(lastBlocked);
            log("inflateStatusData complete");
        });
    }

    public void inflateFluidData() {
        log("=== inflateFluidData() called ===");
        Platform.runLater(() -> {
            log("inflateFluidData - runLater executing...");

            /*

            PiHole pihole1 = null;

            if (piholeDns1 != null)
                pihole1 = piholeDns1.getPiHoleStats();

            PiHole pihole2 = null;
            if (piholeDns2 != null)
                pihole2 = piholeDns2.getPiHoleStats();

            if (pihole1 != null)
                adsPercentage += pihole1.getAds_percentage_today();

            if (pihole2 != null)
                adsPercentage += pihole2.getAds_percentage_today();
            */

            Long queries = 0L;
            Long blockedAds = 0L;


            PiHole pihole1 = null;

            if (piholeDns1 != null) {
                log("Fetching stats from DNS1 for fluid...");
                pihole1 = piholeDns1.getPiHoleStats();
            }

            PiHole pihole2 = null;
            if (piholeDns2 != null) {
                log("Fetching stats from DNS2 for fluid...");
                pihole2 = piholeDns2.getPiHoleStats();
            }

            if ((pihole1 == null || !pihole1.isActive()) && (pihole2 == null || !pihole2.isActive())) {
                log("WARNING: Both Pi-holes inactive, skipping fluid update");
                return;
            }

            if (pihole1 != null) {
                queries += pihole1.getDns_queries_today();
                blockedAds += pihole1.getAds_blocked_today();
            }
            if (pihole2 != null) {
                queries += pihole2.getDns_queries_today();
                blockedAds += pihole2.getAds_blocked_today();
            }

            Double adsPercentage = Double.valueOf(0);

            if (queries != 0L && blockedAds != 0L)
                adsPercentage = (Double.longBitsToDouble(blockedAds) / Double.longBitsToDouble(queries)) * 100;

            log("Ads percentage calculated: " + adsPercentage + "% (queries: " + queries + ", blocked: " + blockedAds + ")");
            fluidTile.setValue(adsPercentage);

            String gravityUpdate = piholeDns1.getGravityLastUpdate();
            log("Gravity last update: " + gravityUpdate);
            fluidTile.setText(gravityUpdate);
            log("inflateFluidData complete");
        });
    }

    public void inflateActiveData() {
        log("=== inflateActiveData() called ===");
        Platform.runLater(() -> {
            log("inflateActiveData - runLater executing...");
            // PiHole pihole = fetchPiholeData();
            PiHole pihole1 = null;

            if (piholeDns1 != null) {
                log("Fetching stats from DNS1 for active check...");
                pihole1 = piholeDns1.getPiHoleStats();
                log("DNS1 - Active: " + (pihole1 != null ? pihole1.isActive() : "null"));
            }

            PiHole pihole2 = null;
            if (piholeDns2 != null) {
                log("Fetching stats from DNS2 for active check...");
                pihole2 = piholeDns2.getPiHoleStats();
                log("DNS2 - Active: " + (pihole2 != null ? pihole2.isActive() : "null"));
            }


            String IPS = "";
            String apiVersion = "";


            if ((pihole1 == null || !pihole1.isActive()) && (pihole2 == null || !pihole2.isActive())) {
                log("WARNING: Both Pi-hole instances are inactive/unavailable - setting LED to RED");
                ledTile.setActiveColor(Color.RED);
                return;
            } else if ((pihole1 != null && pihole1.isActive()) && (pihole2 != null && pihole2.isActive())) {
                log("Both Pi-hole instances are active - setting LED to GREEN");
                ledTile.setActiveColor(Color.LIGHTGREEN);
                IPS += piholeDns1.getIPAddress() + " \n " + piholeDns2.getIPAddress();
            } else if (pihole1 != null && pihole1.isActive() && ((pihole2 == null || !pihole2.isActive()) && piholeDns2 == null)
                    ||
                    (pihole2 != null && pihole2.isActive() && ((pihole1 == null || !pihole1.isActive()) && piholeDns1==null))) {
                log("One Pi-hole instance is active - setting LED to GREEN");
                ledTile.setActiveColor(Color.LIGHTGREEN);
                if (pihole1 == null || !pihole1.isActive()) {
                    IPS += piholeDns2.getIPAddress();
                    log("Getting version from DNS2...");
                    apiVersion = piholeDns2.getVersion();
                }

                if (pihole2 == null || !pihole2.isActive()) {
                    IPS += piholeDns1.getIPAddress();
                    log("Getting version from DNS1...");
                    apiVersion = piholeDns1.getVersion();
                }
            }

            log("Updating LED tile - IPS: " + IPS + ", API Version: " + apiVersion);
            ledTile.setTitle("Widget Version: " + widgetVersion);
            ledTile.setDescription(IPS);
            ledTile.setText("API Version: " + apiVersion);

            ledTile.setTooltipText("Widget Version: " + widgetVersion);
            log("inflateActiveData complete");
        });

    }

    public void inflateLeaderBoardData() {
        log("=== inflateLeaderBoardData() called ===");
        Platform.runLater(() -> {
            log("inflateLeaderBoardData - runLater executing...");

            PiHole pihole1 = null;


            if (piholeDns1 != null) {
                log("Fetching stats from DNS1 for leaderboard...");
                pihole1 = piholeDns1.getPiHoleStats();
            }

            PiHole pihole2 = null;
            if (piholeDns2 != null) {
                log("Fetching stats from DNS2 for leaderboard...");
                pihole2 = piholeDns2.getPiHoleStats();
            }


            if ((pihole1 == null || !pihole1.isActive()) && (pihole2 == null || !pihole2.isActive())) {
                log("WARNING: Both Pi-holes inactive, skipping leaderboard update");
                return;
            }


            log("Fetching top 5 blocked for leaderboard...");
            List<TopAd> topBlocked = piholeDns1.getTopXBlocked(5);

            if (topBlocked == null) {
                log("WARNING: topBlocked is null, skipping leaderboard update");
                return;
            }
            log("Received " + topBlocked.size() + " items for leaderboard");

            String stringToAddAtTheEnd = "..";
            int howMuchToRemove = 20;

            int delay = 200;

            for (int i = 0; i < topBlocked.size(); i++) {

                String domain = topBlocked.get(i).getDomain();

                String domainEdited = domain.length() < howMuchToRemove ? domain : domain.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
                LeaderBoardItem leaderBoardItem = new LeaderBoardItem(domainEdited, topBlocked.get(i).getNumberBlocked());

                Tooltip t = new Tooltip(domain);
                t.setShowDelay(new Duration(delay));

                Tooltip.install(leaderBoardItem, t);

                if (leaderBoardTile.getLeaderBoardItems().size() >= 0 && leaderBoardTile.getLeaderBoardItems().size() < topBlocked.size()) {
                    log("Adding leaderboard item #" + (i+1) + ": " + domainEdited);
                    leaderBoardTile.addLeaderBoardItem(leaderBoardItem);
                } else if (leaderBoardTile.getLeaderBoardItems().size() == topBlocked.size()) {
                    log("Updating leaderboard item #" + (i+1) + ": " + domain);
                    leaderBoardTile.getLeaderBoardItems().get(i).setName(domain);
                    leaderBoardTile.getLeaderBoardItems().get(i).setValue(topBlocked.get(i).getNumberBlocked());
                }

            }
            log("inflateLeaderBoardData complete");
        });
    }

    public void inflateTopXData() {
        log("=== inflateTopXData() called ===");
        Platform.runLater(() -> {
            log("inflateTopXData - runLater executing...");

            PiHole pihole1 = null;

            if (piholeDns1 != null) {
                log("Fetching stats from DNS1 for topX...");
                pihole1 = piholeDns1.getPiHoleStats();
            }

            PiHole pihole2 = null;
            if (piholeDns2 != null) {
                log("Fetching stats from DNS2 for topX...");
                pihole2 = piholeDns2.getPiHoleStats();
            }


            if ((pihole1 == null || !pihole1.isActive()) && (pihole2 == null || !pihole2.isActive())) {
                log("WARNING: Both Pi-holes inactive, skipping topX update");
                return;
            }

            log("Fetching top " + topX + " blocked domains...");
            List<TopAd> topBlocked = piholeDns1.getTopXBlocked(topX);

            if (topBlocked == null) {
                log("WARNING: topBlocked is null, skipping update");
                return;
            }
            log("Received " + topBlocked.size() + " blocked domains");

            String stringToAddAtTheEnd = "..";
            int howMuchToRemove = 20;

            Label name = new Label("Domain");
            name.setTextFill(Tile.FOREGROUND);
            name.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(name, Priority.NEVER);


            Label views = new Label("Nbr BLocks");
            views.setTextFill(Tile.FOREGROUND);
            views.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(views, Priority.NEVER);


            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox header = new HBox(5, name, spacer, views);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setFillHeight(true);

            Region spacer2 = new Region();
            spacer2.setPrefSize(5, 5);
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            HBox header2 = new HBox(5, spacer2);
            header2.setAlignment(Pos.CENTER_LEFT);
            header2.setFillHeight(true);


            dataTable.getChildren().setAll(header, header2);
            log("DataTable header set, processing " + topBlocked.size() + " blocked domains...");

            for (int i = 0; i < topBlocked.size(); i++) {

                String domain = topBlocked.get(i).getDomain();
                String domainEdited = domain.length() < howMuchToRemove ? domain : domain.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
                Long blockCount = topBlocked.get(i).getNumberBlocked();
                log("Processing topX item " + (i+1) + "/" + topBlocked.size() + ": " + domainEdited + " (" + blockCount + " blocks)");

                HBox domainHbox = getTopBlockedItem(i + 1, domain, domainEdited, blockCount.toString());
                dataTable.getChildren().add(domainHbox);
            }

            topXTile.setGraphic(dataTable);
            log("TopX tile graphic updated with " + topBlocked.size() + " items");
            log("inflateTopXData complete");
        });
    }

    public void inflateTopXData__() {
        Platform.runLater(() -> {

            PiHole pihole1 = null;


            if (piholeDns1 != null)
                pihole1 = piholeDns1.getPiHoleStats();

            PiHole pihole2 = null;
            if (piholeDns2 != null)
                pihole2 = piholeDns2.getPiHoleStats();


            if ((pihole1 == null || !pihole1.isActive()) && (pihole2 == null || !pihole2.isActive()))
                return;

            List<TopAd> topBlocked = piholeDns1.getTopXBlocked(topX);

            String stringToAddAtTheEnd = "..";
            int howMuchToRemove = 20;

            String domain1, domain2, domain3, domain4, domain5;
            String domain1Edited, domain2Edited, domain3Edited, domain4Edited, domain5Edited;

            domain1 = topBlocked.get(0).getDomain();
            domain2 = topBlocked.get(1).getDomain();
            domain3 = topBlocked.get(2).getDomain();
            domain4 = topBlocked.get(3).getDomain();
            domain5 = topBlocked.get(4).getDomain();

            domain1Edited = domain1.length() < howMuchToRemove ? domain1 : domain1.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
            domain2Edited = domain2.length() < howMuchToRemove ? domain2 : domain2.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
            domain3Edited = domain3.length() < howMuchToRemove ? domain3 : domain3.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
            domain4Edited = domain4.length() < howMuchToRemove ? domain4 : domain4.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
            domain5Edited = domain5.length() < howMuchToRemove ? domain5 : domain5.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);


            Label name = new Label("Domain");
            name.setTextFill(Tile.FOREGROUND);
            name.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(name, Priority.NEVER);


            Label views = new Label("Nbr BLocks");
            views.setTextFill(Tile.FOREGROUND);
            views.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(views, Priority.NEVER);


            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox header = new HBox(5, name, spacer, views);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setFillHeight(true);

            Region spacer2 = new Region();
            spacer2.setPrefSize(5, 5);
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            HBox header2 = new HBox(5, spacer2);
            header2.setAlignment(Pos.CENTER_LEFT);
            header2.setFillHeight(true);


            HBox domain1Hbox = getTopBlockedItem(1, domain1, domain1Edited, topBlocked.get(0).getNumberBlocked().toString());
            HBox domain2Hbox = getTopBlockedItem(2, domain2, domain2Edited, topBlocked.get(1).getNumberBlocked().toString());
            HBox domain3Hbox = getTopBlockedItem(3, domain3, domain3Edited, topBlocked.get(2).getNumberBlocked().toString());
            HBox domain4Hbox = getTopBlockedItem(4, domain4, domain4Edited, topBlocked.get(3).getNumberBlocked().toString());
            HBox domain5Hbox = getTopBlockedItem(5, domain5, domain5Edited, topBlocked.get(4).getNumberBlocked().toString());


            dataTable.getChildren().setAll(header, header2);//, domain1Hbox, domain2Hbox, domain3Hbox, domain4Hbox, domain5Hbox);
            dataTable.getChildren().add(domain1Hbox);
            dataTable.getChildren().add(domain2Hbox);
            dataTable.getChildren().add(domain3Hbox);
            dataTable.getChildren().add(domain4Hbox);
            dataTable.getChildren().add(domain5Hbox);

            dataTable.setFillWidth(true);
            dataTable.setAlignment(Pos.CENTER);


            topXTile.setTitle("Top " + String.valueOf(topX) + " Blocked");
            topXTile.setGraphic(dataTable);

        });
    }

    private HBox getTopBlockedItem(int num, final String domain, final String editedDomain, final String data) {
        log("getTopBlockedItem() - Creating item #" + num + ": " + editedDomain + " (" + data + " blocks)");

        ImageView iv1 = null;
        try {
            String imagePath = System.getProperty("user.dir") + "/src/main/resources/media/images/" + num + ".png";
            log("getTopBlockedItem() - Loading image from: " + imagePath);
            FileInputStream input = new FileInputStream(imagePath);
            Image image = new Image(input);
            iv1 = new ImageView();
            iv1.setImage(image);
            iv1.setFitHeight(10);
            iv1.setFitWidth(10);
            log("getTopBlockedItem() - Image loaded successfully");
        } catch (Exception e) {
            log("getTopBlockedItem() - WARNING: Failed to load image: " + e.getMessage());
            System.out.println(e);
        }

        Label domainLabel = new Label(editedDomain);
        domainLabel.setTextFill(Tile.FOREGROUND);
        domainLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(domainLabel, Priority.NEVER);

        Region spacer = new Region();
        spacer.setPrefSize(5, 5);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(data);
        valueLabel.setTextFill(Tile.FOREGROUND);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(valueLabel, Priority.NEVER);


        Tooltip t1 = new Tooltip(domain);
        t1.setShowDelay(new Duration(200));

        Tooltip.install(domainLabel, t1);

        HBox hBox;
        if (iv1 != null)
            hBox = new HBox(5, iv1, domainLabel, spacer, valueLabel);
        else
            hBox = new HBox(5, domainLabel, spacer, valueLabel);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setFillHeight(true);

        log("getTopBlockedItem() - Item #" + num + " created");
        return hBox;

    }

    private void initTiles() {
        log("=== initTiles() called ===");

        log("Creating Fluid tile at (0, 0)...");
        initFluidTile(0, 0);
        log("Fluid tile created");

        log("Creating LED tile at (" + TILE_WIDTH + ", 0)...");
        initLEDTile(TILE_WIDTH, 0);
        log("LED tile created");

        log("Creating Status tile at (0, " + TILE_HEIGHT + ")...");
        initStatusTile(0, TILE_HEIGHT, "Nbr of domains blocked: ", "", "Processed", "Blocked", "Accepted", "Gravity");
        log("Status tile created");

        log("Creating Custom tile (TopX)...");
        initCustomTile();
        log("Custom tile created");
        
        log("=== initTiles() complete ===");
    }

    private void initRadialTile() {
        /*--Other Percentage Tile--*/
        /*

        chartData1 = new ChartData("Item 1", 24.0, Tile.GREEN);
        chartData2 = new ChartData("Item 2", 10.0, Tile.BLUE);
        chartData3 = new ChartData("Item 3", 12.0, Tile.RED);

        radialPercentageTile = TileBuilder.create().skinType(Tile.SkinType.RADIAL_PERCENTAGE)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                //.backgroundColor(Color.web("#26262D"))
                .maxValue(1000)
                .title("RadialPercentage Tile")
                .description("Product 1")
                .textVisible(false)
                .chartData(chartData1, chartData2, chartData3)
                .animated(true)
                .referenceValue(100)
                .value(chartData1.getValue())
                .descriptionColor(Tile.GRAY)
                //.valueColor(Tile.BLUE)
                //.unitColor(Tile.BLUE)
                .barColor(Tile.BLUE)
                .decimals(0)
                .build();

        radialPercentageTile.setNotifyRegionTooltipText("tooltip");
        radialPercentageTile.showNotifyRegion(true);*/
    }

    private void initFluidTile(double x, double y) {
        log("initFluidTile() - Building FLUID tile with size " + TILE_WIDTH + "x" + TILE_HEIGHT);
        /*--Fluid Percentage Tile--*/
        fluidTile = TileBuilder.create().skinType(Tile.SkinType.FLUID).prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Gravity last update: ")
                .text("ADS Blocked")
                .unit("\u0025").decimals(0).barColor(Tile.RED) // defines the fluid color, alternatively use sections or gradientstops
                .animated(true).build();

        //fluidTile.setLayoutX(x);
        //fluidTile.setLayoutY(y);
        fluidTile.setValue(0);
        log("initFluidTile() - Fluid tile built, initial value set to 0");
    }

    private void initLEDTile(double x, double y) {
        log("initLEDTile() - Building LED tile with size " + TILE_WIDTH + "x" + TILE_HEIGHT);
        /*--LED Tile--*/
        ledTile = TileBuilder.create().skinType(Tile.SkinType.LED).prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Version: ")
                .description("Description")
                .text("Whatever text").build();
        //ledTile.setLayoutX(x);
        // ledTile.setLayoutY(y);
        ledTile.setActive(true);
        log("initLEDTile() - LED tile built, active set to true");
    }

    private void initLeaderBoard2(int TopX) {

        leaderBoardTile = TileBuilder.create()
                .skinType(Tile.SkinType.LEADER_BOARD)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Top " + TopX + " Blocked")
                .text("Whatever text")
                //.textSize(Tile.TextSize.SMALLER)
                .build();

        leaderBoardTile.setText("Copyright (C) " + Year.now().getValue() + ".  Reda ELFARISSI aka foxy999");

        List<TopAd> topBlocked = piholeDns1.getTopXBlocked(TopX);

        String stringToAddAtTheEnd = "..";
        int howMuchToRemove = 20;

        String domain1, domain2, domain3, domain4, domain5;
        String domain1Edited, domain2Edited, domain3Edited, domain4Edited, domain5Edited;

        domain1 = topBlocked.get(0).getDomain();
        domain2 = topBlocked.get(1).getDomain();
        domain3 = topBlocked.get(2).getDomain();
        domain4 = topBlocked.get(3).getDomain();
        domain5 = topBlocked.get(4).getDomain();

        domain1Edited = domain1.length() < howMuchToRemove ? domain1 : domain1.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
        domain2Edited = domain2.length() < howMuchToRemove ? domain2 : domain2.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
        domain3Edited = domain3.length() < howMuchToRemove ? domain3 : domain3.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
        domain4Edited = domain4.length() < howMuchToRemove ? domain4 : domain4.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);
        domain5Edited = domain5.length() < howMuchToRemove ? domain5 : domain5.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);

        // LeaderBoard Items
        LeaderBoardItem leaderBoardItem1 = new LeaderBoardItem(domain1Edited, topBlocked.get(0).getNumberBlocked());
        LeaderBoardItem leaderBoardItem2 = new LeaderBoardItem(domain2Edited, topBlocked.get(1).getNumberBlocked());
        LeaderBoardItem leaderBoardItem3 = new LeaderBoardItem(domain3Edited, topBlocked.get(2).getNumberBlocked());
        LeaderBoardItem leaderBoardItem4 = new LeaderBoardItem(domain4Edited, topBlocked.get(3).getNumberBlocked());
        LeaderBoardItem leaderBoardItem5 = new LeaderBoardItem(domain5Edited, topBlocked.get(4).getNumberBlocked());

        int i = 200;

        Tooltip t1 = new Tooltip(domain1);
        t1.setShowDelay(new Duration(i));

        Tooltip t2 = new Tooltip(domain1);
        t2.setShowDelay(new Duration(i));

        Tooltip t3 = new Tooltip(domain1);
        t3.setShowDelay(new Duration(i));

        Tooltip t4 = new Tooltip(domain1);
        t4.setShowDelay(new Duration(i));

        Tooltip t5 = new Tooltip(domain1);
        t5.setShowDelay(new Duration(i));

        Tooltip.install(leaderBoardItem1, t1);
        Tooltip.install(leaderBoardItem2, t2);
        Tooltip.install(leaderBoardItem3, t3);
        Tooltip.install(leaderBoardItem4, t4);
        Tooltip.install(leaderBoardItem5, t5);


        //leaderBoardItem1.add
        leaderBoardTile.addLeaderBoardItem(leaderBoardItem1);
        leaderBoardTile.addLeaderBoardItem(leaderBoardItem2);
        leaderBoardTile.addLeaderBoardItem(leaderBoardItem3);
        leaderBoardTile.addLeaderBoardItem(leaderBoardItem4);
        leaderBoardTile.addLeaderBoardItem(leaderBoardItem5);

    }

    private void initLeaderBoard(int TopX) {


        leaderBoardTile = TileBuilder.create()
                .skinType(Tile.SkinType.LEADER_BOARD)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Top " + TopX + " Blocked")
                .text("Whatever text")
                //.textSize(Tile.TextSize.SMALLER)
                .build();

        leaderBoardTile.setText("Copyright (C) " + Year.now().getValue() + ".  Reda ELFARISSI aka foxy999");

        //gridPane = new FlowGridPane(cols, rows, ledTile, fluidTile, statusTile, leaderBoardTile);
/*
        leaderBoardTile.setActive(false);
        leaderBoardTile.setAnimated(true);
        leaderBoardTile.updateLocation(leaderBoardTile.getLayoutX(),leaderBoardTile.getLayoutY());*/

    }

    private void initCustomTile() {
        log("initCustomTile() - Building CUSTOM tile for Top " + topX + " Blocked");
        /*
        Label name = new Label("Domain");
        name.setTextFill(Tile.FOREGROUND);
        name.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(name, Priority.NEVER);

        Region spacer = new Region();
        spacer.setPrefSize(5, 5);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label views = new Label("Nbr BLocks");
        views.setTextFill(Tile.FOREGROUND);
        views.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(views, Priority.NEVER);

        HBox header = new HBox(5, name, spacer, views);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setFillHeight(true);


        Region spacer2 = new Region();
        spacer2.setPrefSize(5, 5);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox header2 = new HBox(5, spacer2);
        header2.setAlignment(Pos.CENTER_LEFT);
        header2.setFillHeight(true);

*/
        dataTable = new VBox();
        dataTable.setFillWidth(true);
        dataTable.setAlignment(Pos.CENTER_LEFT);
        log("initCustomTile() - DataTable VBox created");

        topXTile = TileBuilder.create()
                .skinType(Tile.SkinType.CUSTOM).prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("TOP X")
                .text(("Copyright (C) " + Year.now().getValue() + ".  Reda ELFARISSI aka foxy999"))
                //.graphic(new VBox())
                .build();

        topXTile.setTitle("Top " + String.valueOf(topX) + " Blocked");
        log("initCustomTile() - TopX tile built with title 'Top " + topX + " Blocked'");
    }

    private void initStatusTile(double x, double y, String statusTitle, String notifications, String leftText, String middleText, String rightText, String text) {
        log("initStatusTile() - Building STATUS tile with size " + TILE_WIDTH + "x" + TILE_HEIGHT);
        log("initStatusTile() - Title: '" + statusTitle + "', Left: '" + leftText + "', Middle: '" + middleText + "', Right: '" + rightText + "'");

        Indicator leftGraphics;
        Indicator middleGraphics;
        Indicator rightGraphics;
        /*--Status Tile--*/
        leftGraphics = new Indicator(Tile.BLUE);
        leftGraphics.setOn(true);

        middleGraphics = new Indicator(Tile.RED);
        middleGraphics.setOn(true);

        rightGraphics = new Indicator(Tile.GREEN);
        rightGraphics.setOn(true);
        log("initStatusTile() - Indicators created (Blue, Red, Green)");

        statusTile = TileBuilder.create().skinType(Tile.SkinType.STATUS).prefSize(TILE_WIDTH, TILE_HEIGHT).
                title(statusTitle).
                description(notifications).
                leftText(leftText).
                middleText(middleText).
                rightText(rightText).leftGraphics(leftGraphics).middleGraphics(middleGraphics).rightGraphics(rightGraphics).
                text(text).build();

        // statusTile.setLayoutX(x);
        // statusTile.setLayoutY(y);

        statusTile.setLeftValue(0);
        statusTile.setMiddleValue(0);
        statusTile.setRightValue(0);
        log("initStatusTile() - Status tile built, initial values set to 0");
    }

    private void initializeContextMenu() {
        log("=== initializeContextMenu() called ===");

        MenuItem hideToTrayItem = new MenuItem("Hide to Tray");
        hideToTrayItem.setOnAction(event -> {
            log("Context menu: 'Hide to Tray' clicked");
            WidgetApplication.hideToTray();
        });

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(event -> {
            log("Context menu: 'Exit' clicked - shutting down");
            System.exit(0);
        });

        MenuItem refreshItem = new MenuItem("Refresh All Now");
        refreshItem.setOnAction(event -> {
            log("Context menu: 'Refresh All Now' clicked");
            inflateAllData();
        });

        MenuItem configItem = new MenuItem("Settings");
        configItem.setOnAction(event -> {
            log("Context menu: 'Settings' clicked");
            WidgetApplication.openConfigurationWindow();
        });

        MenuItem testItem = new MenuItem("Test");
        testItem.setOnAction(event -> {
            log("Context menu: 'Test' clicked");
        });

        log("Menu items created: Hide to Tray, Exit, Refresh All Now, Settings");

        final ContextMenu contextMenu = new ContextMenu(hideToTrayItem, refreshItem, configItem, exitItem
                //, testItem
        );
        
        log("Attaching context menu to gridPane...");
        gridPane.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isSecondaryButtonDown()) {
                log("Right-click detected on gridPane, showing context menu");
                contextMenu.show(gridPane, event.getScreenX(), event.getScreenY());
            } else {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                }
            }
        });

        log("Attaching context menu to " + gridPane.getChildren().size() + " child nodes...");
        for (Node truc : gridPane.getChildren()) {


            truc.setOnMousePressed(event -> {
                if (event.isSecondaryButtonDown()) {
                    contextMenu.show(gridPane, event.getScreenX(), event.getScreenY());
                } else {
                    if (contextMenu.isShowing()) {
                        contextMenu.hide();
                    }
                }
            });
            truc.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.isSecondaryButtonDown()) {
                    contextMenu.show(gridPane, event.getScreenX(), event.getScreenY());
                } else {
                    if (contextMenu.isShowing()) {
                        contextMenu.hide();
                    }
                }
            });
        }
        log("=== initializeContextMenu() complete ===");
    }

    public PiholeConfig getConfigDNS1() {
        log("getConfigDNS1() - returning: " + (configDNS1 != null ? configDNS1.getIPAddress() : "null"));
        return configDNS1;
    }

    public PiholeConfig getConfigDNS2() {
        log("getConfigDNS2() - returning: " + (configDNS2 != null ? configDNS2.getIPAddress() : "null"));
        return configDNS2;
    }

    public void setConfigDNS1(PiholeConfig configDNS1) {
        log("setConfigDNS1() - setting to: " + (configDNS1 != null ? configDNS1.getIPAddress() + ":" + configDNS1.getPort() : "null"));
        this.configDNS1 = configDNS1;
    }

    public void setConfigDNS2(PiholeConfig configDNS2) {
        log("setConfigDNS2() - setting to: " + (configDNS2 != null ? configDNS2.getIPAddress() + ":" + configDNS2.getPort() : "null"));
        this.configDNS2 = configDNS2;
    }

    public WidgetConfig getWidgetConfig() {
        log("getWidgetConfig() - returning: " + (widgetConfig != null ? "size=" + widgetConfig.getSize() + ", layout=" + widgetConfig.getLayout() : "null"));
        return widgetConfig;
    }

    public void setWidgetConfig(WidgetConfig widgetConfig) {
        log("setWidgetConfig() - setting to: " + (widgetConfig != null ? "size=" + widgetConfig.getSize() + ", layout=" + widgetConfig.getLayout() : "null"));
        this.widgetConfig = widgetConfig;
    }
}