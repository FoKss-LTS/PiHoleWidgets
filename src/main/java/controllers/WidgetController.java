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
import services.helpers.HelperService;

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


    @FXML
    private Pane rootPane;

    private FlowGridPane gridPane;

    @FXML
    private Label dakLabel;

    @FXML
    public void openConfigurationWindow() {
        WidgetApplication.openConfigurationWindow();
    }

    public WidgetController(PiholeConfig configDNS1, PiholeConfig configDNS2, WidgetConfig widgetConfig) {
        this.configDNS1 = configDNS1;
        this.configDNS2 = configDNS2;
        this.widgetConfig = widgetConfig;
    }

    public void initialize(URL location, ResourceBundle resources) {

        if (configDNS1 != null || configDNS2 != null) {


            topX = 5;

            if (widgetConfig != null) {
                switch (widgetConfig.getSize()) {
                    case "Small":
                        System.out.println("Small");
                        TILE_WIDTH = 150;
                        TILE_HEIGHT = 150;
                        break;
                    case "Medium":
                        TILE_WIDTH = 200;
                        TILE_HEIGHT = 200;
                        break;
                    case "Large":
                        TILE_WIDTH = 350;
                        TILE_HEIGHT = 350;
                        break;
                    case "XXL":
                        System.out.println("XL");
                        TILE_WIDTH = 500;
                        TILE_HEIGHT = 500;
                        break;
                    case "Full Screen":
                        TILE_WIDTH = Screen.getPrimary().getBounds().getMaxX() / 4;
                        TILE_HEIGHT = Screen.getPrimary().getBounds().getMaxX() / 4;
                        break;
                    default:
                        TILE_WIDTH = 200;
                        TILE_HEIGHT = 200;
                }

                switch (widgetConfig.getLayout()) {
                    case "Horizontal":
                        cols = 4;
                        rows = 1;
                        break;
               /* case "Vertical":
                    cols = 1;
                    rows = 4;
                    break;*/
                    case "Square":
                        cols = 2;
                        rows = 2;
                        break;
                    default:
                        cols = 2;
                        rows = 2;
                }
            }


            refreshPihole();

            initTiles();

            initializeStatusScheduler();
            initializeActiveTileScheduler();
            initializeFluidTileScheduler();
            initializeTopXBlockedScheduler();


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

            gridPane = new FlowGridPane(cols, rows, ledTile, fluidTile, statusTile, topXTile);
            gridPane.setHgap(5);
            gridPane.setVgap(5);
            gridPane.setAlignment(Pos.CENTER);
            gridPane.setCenterShape(true);
            gridPane.setPadding(new Insets(5));
            //gridPane.setPrefSize(TILE_WIDTH*2, 600);
            gridPane.setBackground(new Background(new BackgroundFill(Color.web("#101214"), CornerRadii.EMPTY, Insets.EMPTY)));

            /*
            rootPane.getChildren().add(gridPane);
            rootPane.setPrefSize(TILE_WIDTH * 2, TILE_HEIGHT * 2);
            rootPane.getChildren().add(fluidTile);
            rootPane.getChildren().add(ledTile);
            rootPane.getChildren().add(statusTile);
            */
            initializeContextMenu();
        } else {
            System.out.println("configurations are empty");
        }

    }

    public void refreshPihole() {
        if (configDNS1 != null)
            piholeDns1 = new PiHoleHandler(configDNS1.getIPAddress(), configDNS1.getPort(), configDNS1.getScheme(), configDNS1.getAUTH());

        if (configDNS2 != null)
            piholeDns2 = new PiHoleHandler(configDNS2.getIPAddress(), configDNS2.getPort(), configDNS2.getScheme(), configDNS2.getAUTH());

        inflateAllData();
    }

    private void inflateAllData() {
        inflateActiveData();
        inflateFluidData();
        inflateStatusData();
        inflateTopXData();
    }

    public FlowGridPane getGridPane() {
        return gridPane;
    }

    private void initializeStatusScheduler() {
        ScheduledExecutorService executorStatusService = Executors.newSingleThreadScheduledExecutor();
        executorStatusService.scheduleAtFixedRate(this::inflateStatusData, 0, 5, TimeUnit.SECONDS);
    }

    private void initializeFluidTileScheduler() {
        ScheduledExecutorService executorFluidService = Executors.newSingleThreadScheduledExecutor();
        executorFluidService.scheduleAtFixedRate(this::inflateFluidData, 0, 15, TimeUnit.SECONDS);
    }

    private void initializeActiveTileScheduler() {
        ScheduledExecutorService executorActiveService = Executors.newSingleThreadScheduledExecutor();
        executorActiveService.scheduleAtFixedRate(this::inflateActiveData, 0, 60, TimeUnit.SECONDS);
    }

    private void initializeTopXBlockedScheduler() {
        ScheduledExecutorService executorLeaderBoardService = Executors.newSingleThreadScheduledExecutor();
        executorLeaderBoardService.scheduleAtFixedRate(this::inflateTopXData, 0, 5, TimeUnit.SECONDS);
    }

    public void inflateStatusData() {
        Platform.runLater(() -> {

            Long queries = 0L;
            Long blockedAds = 0L;
            Long queriesProcessed = 0L;
            Long domainsBlocked = 0L;


            PiHole pihole1 = null;

            if (piholeDns1 != null)
                pihole1 = piholeDns1.getPiHoleStats();

            PiHole pihole2 = null;
            if (piholeDns2 != null)
                pihole2 = piholeDns2.getPiHoleStats();


            if (pihole1 != null) {
                queries += pihole1.getDns_queries_today();
                blockedAds += pihole1.getAds_blocked_today();
                queriesProcessed += pihole1.getQueries_forwarded();
                queriesProcessed += pihole1.getQueries_cached();
                domainsBlocked = pihole1.getDomains_being_blocked();
            }
            if (pihole2 != null) {
                queries += pihole2.getDns_queries_today();
                blockedAds += pihole2.getAds_blocked_today();
                queriesProcessed += pihole2.getQueries_forwarded();
                queriesProcessed += pihole2.getQueries_cached();

            }

            statusTile.setLeftValue(queries);
            statusTile.setMiddleValue(blockedAds);
            statusTile.setRightValue(queriesProcessed);

            statusTile.setDescription(HelperService.getHumanReadablePriceFromNumber(domainsBlocked));

            statusTile.setText(piholeDns1.getLastBlocked());

        });
    }

    public void inflateFluidData() {
        Platform.runLater(() -> {

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

            if (piholeDns1 != null)
                pihole1 = piholeDns1.getPiHoleStats();

            PiHole pihole2 = null;
            if (piholeDns2 != null)
                pihole2 = piholeDns2.getPiHoleStats();

            if ((pihole1 == null || !pihole1.isActive()) && (pihole2 == null || !pihole2.isActive())) {
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

            fluidTile.setValue(adsPercentage);

            fluidTile.setText(piholeDns1.getGravityLastUpdate());

        });
    }

    public void inflateActiveData() {
        Platform.runLater(() -> {
            // PiHole pihole = fetchPiholeData();
            PiHole pihole1 = null;

            if (piholeDns1 != null)
                pihole1 = piholeDns1.getPiHoleStats();

            PiHole pihole2 = null;
            if (piholeDns2 != null)
                pihole2 = piholeDns2.getPiHoleStats();


            String IPS = "";
            String apiVersion = "";


            if ((pihole1 == null || !pihole1.isActive()) && (pihole2 == null || !pihole2.isActive())) {
                ledTile.setActiveColor(Color.RED);
                return;
            } else if ((pihole1 != null && pihole1.isActive()) && (pihole2 != null && pihole2.isActive())) {
                ledTile.setActiveColor(Color.LIGHTGREEN);
                IPS += piholeDns1.getIPAddress() + " \n " + piholeDns2.getIPAddress();
            } else if (pihole1 != null && pihole1.isActive() && ((pihole2 == null || !pihole2.isActive()) && piholeDns2 == null)
                    ||
                    (pihole2 != null && pihole2.isActive() && ((pihole1 == null || !pihole1.isActive()) && piholeDns1==null))) {
                ledTile.setActiveColor(Color.LIGHTGREEN);
                if (pihole1 == null || !pihole1.isActive()) {
                    IPS += piholeDns2.getIPAddress();
                    apiVersion = piholeDns2.getVersion();
                }

                if (pihole2 == null || !pihole2.isActive()) {
                    IPS += piholeDns1.getIPAddress();
                    apiVersion = piholeDns1.getVersion();
                }
            }


            ledTile.setTitle("Widget Version: " + widgetVersion);
            ledTile.setDescription(IPS);
            ledTile.setText("API Version: " + apiVersion);

            ledTile.setTooltipText("Widget Version: " + widgetVersion);

        });

    }

    public void inflateLeaderBoardData() {
        Platform.runLater(() -> {

            PiHole pihole1 = null;


            if (piholeDns1 != null)
                pihole1 = piholeDns1.getPiHoleStats();

            PiHole pihole2 = null;
            if (piholeDns2 != null)
                pihole2 = piholeDns2.getPiHoleStats();


            if ((pihole1 == null || !pihole1.isActive()) && (pihole2 == null || !pihole2.isActive()))
                return;


            List<TopAd> topBlocked = piholeDns1.getTopXBlocked(5);

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

                    leaderBoardTile.addLeaderBoardItem(leaderBoardItem);
                } else if (leaderBoardTile.getLeaderBoardItems().size() == topBlocked.size()) {
                    leaderBoardTile.getLeaderBoardItems().get(i).setName(domain);
                    leaderBoardTile.getLeaderBoardItems().get(i).setValue(topBlocked.get(i).getNumberBlocked());
                }

            }

        });
    }

    public void inflateTopXData() {
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

            if (topBlocked == null)
                return;

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

            for (int i = 0; i < topBlocked.size(); i++) {

                String domain = topBlocked.get(i).getDomain();
                String domainEdited = domain.length() < howMuchToRemove ? domain : domain.substring(0, howMuchToRemove).concat(stringToAddAtTheEnd);

                HBox domainHbox = getTopBlockedItem(i + 1, domain, domainEdited, topBlocked.get(i).getNumberBlocked().toString());
                dataTable.getChildren().add(domainHbox);
            }

            topXTile.setGraphic(dataTable);

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

        ImageView iv1 = null;
        try {


            FileInputStream input = new FileInputStream(System.getProperty("user.dir") + "/src/main/resources/media/images/" + num + ".png");
            Image image = new Image(input);
            iv1 = new ImageView();
            iv1.setImage(image);
            iv1.setFitHeight(10);
            iv1.setFitWidth(10);
        } catch (Exception e) {
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

        return hBox;

    }

    private void initTiles() {

        initFluidTile(0, 0);

        initLEDTile(TILE_WIDTH, 0);

        initStatusTile(0, TILE_HEIGHT, "Nbr of domains blocked: ", "", "Processed", "Blocked", "Accepted", "Gravity");

        initCustomTile();
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
        /*--Fluid Percentage Tile--*/
        fluidTile = TileBuilder.create().skinType(Tile.SkinType.FLUID).prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Gravity last update: ")
                .text("ADS Blocked")
                .unit("\u0025").decimals(0).barColor(Tile.RED) // defines the fluid color, alternatively use sections or gradientstops
                .animated(true).build();

        //fluidTile.setLayoutX(x);
        //fluidTile.setLayoutY(y);
        fluidTile.setValue(0);
    }

    private void initLEDTile(double x, double y) {
        /*--LED Tile--*/
        ledTile = TileBuilder.create().skinType(Tile.SkinType.LED).prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Version: ")
                .description("Description")
                .text("Whatever text").build();
        //ledTile.setLayoutX(x);
        // ledTile.setLayoutY(y);
        ledTile.setActive(true);
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

        topXTile = TileBuilder.create()
                .skinType(Tile.SkinType.CUSTOM).prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("TOP X")
                .text(("Copyright (C) " + Year.now().getValue() + ".  Reda ELFARISSI aka foxy999"))
                //.graphic(new VBox())
                .build();

        topXTile.setTitle("Top " + String.valueOf(topX) + " Blocked");
    }

    private void initStatusTile(double x, double y, String statusTitle, String notifications, String leftText, String middleText, String rightText, String text) {

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
    }

    private void initializeContextMenu() {

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(event -> {
            System.exit(0);
        });

        MenuItem refreshItem = new MenuItem("Refresh All Now");
        refreshItem.setOnAction(event -> {
            inflateAllData();
        });

        MenuItem configItem = new MenuItem("Settings");
        configItem.setOnAction(event -> {
            WidgetApplication.openConfigurationWindow();
        });

        MenuItem testItem = new MenuItem("Test");
        testItem.setOnAction(event -> {
        });

        final ContextMenu contextMenu = new ContextMenu(exitItem, refreshItem, configItem
                //, testItem
        );
        gridPane.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isSecondaryButtonDown()) {
                contextMenu.show(gridPane, event.getScreenX(), event.getScreenY());
            } else {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                }
            }
        });

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

    }

    public PiholeConfig getConfigDNS1() {
        return configDNS1;
    }

    public PiholeConfig getConfigDNS2() {
        return configDNS2;
    }

    public void setConfigDNS1(PiholeConfig configDNS1) {
        this.configDNS1 = configDNS1;
    }

    public void setConfigDNS2(PiholeConfig configDNS2) {
        this.configDNS2 = configDNS2;
    }

    public WidgetConfig getWidgetConfig() {
        return widgetConfig;
    }

    public void setWidgetConfig(WidgetConfig widgetConfig) {
        this.widgetConfig = widgetConfig;
    }
}