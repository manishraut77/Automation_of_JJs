package com.jjcorner.app.controller.busboy;

import com.jjcorner.app.AppContext;
import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.RestaurantTable;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.model.TableStatus;
import com.jjcorner.app.nav.SceneManager;
import com.jjcorner.app.util.AlertHelper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public final class BusboyHomeController implements Initializable {
    @FXML private Label titleLabel;
    @FXML private Button clockButton;
    @FXML private Label elapsedLabel;
    @FXML private GridPane floorGrid;
    @FXML private Label selectedTableLabel;
    @FXML private Label selectedStatusLabel;
    @FXML private Button markOpenBtn;
    @FXML private Label hintLabel;
    @FXML private Label clockLabel;
    @FXML private ImageView logoImage;

    private final Map<String, Button> tableButtons = new HashMap<>();
    private RestaurantTable selectedTable;
    private Timeline elapsedTimer;
    private static final ZoneId EASTERN = ZoneId.of("America/New_York");
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Employee u = AppContext.session().currentUser();
        titleLabel.setText("Busboy - " + (u == null ? "" : u.displayName()));

        buildFloorGrid();
        setSelected(null);
        refreshClockUi();

        elapsedTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshClockUi()));
        elapsedTimer.setCycleCount(Timeline.INDEFINITE);
        elapsedTimer.play();

        hintLabel.setText("Dirty tables are red. Clock in to mark Dirty -> Ready.");
        if (logoImage != null && logoImage.getImage() == null) {
            var url = getClass().getResource("/com/jjcorner/view/jj-logo.png");
            if (url != null) logoImage.setImage(new Image(url.toExternalForm()));
        }
    }

    public void onHome() {
        setSelected(null);
    }

    public void onClockToggle() {
        if (!AppContext.session().isClockedIn()) {
            AppContext.clock().clockIn();
        } else {
            boolean confirmed = AlertHelper.confirm("Clock out", "Clock out now?");
            if (!confirmed) return;
            AppContext.clock().clockOut();
            setSelected(null);
        }
        refreshClockUi();
    }

    public void onLogout() {
        SceneManager.logout();
    }

    public void onSwitchUser() {
        SceneManager.switchUser();
    }

    public void onMarkOpen() {
        if (selectedTable == null) return;
        if (!AppContext.session().isClockedIn()) {
            AlertHelper.error("Protected action", "Error: You are not clocked in");
            return;
        }
        try {
            AppContext.tables().attemptStatusChange(Role.BUSBOY, selectedTable, TableStatus.OPEN);
        } catch (RuntimeException ex) {
            AlertHelper.error("Status change", ex.getMessage());
        }
        refreshSelectedPanel();
    }

    private void buildFloorGrid() {
        floorGrid.getChildren().clear();
        tableButtons.clear();
        floorGrid.setHgap(18);
        floorGrid.setVgap(16);

        for (int row = 1; row <= 6; row++) {
            Label rowLbl = new Label(String.valueOf(row));
            rowLbl.setStyle("-fx-font-weight: bold;");
            floorGrid.add(rowLbl, 0, row);
        }
        char[] cols = {'A','B','C','D','E','F'};
        for (int i = 0; i < cols.length; i++) {
            Label colLbl = new Label(String.valueOf(cols[i]));
            colLbl.setStyle("-fx-font-weight: bold;");
            floorGrid.add(colLbl, i + 1, 7);
        }

        for (char col : new char[]{'A','B','E','F'}) {
            for (int row = 1; row <= 6; row++) {
                addTableButton(col, row);
            }
        }
        for (char col : new char[]{'C','D'}) {
            for (int row = 5; row <= 6; row++) {
                addTableButton(col, row);
            }
        }

        VBox kitchen = new VBox();
        kitchen.setMinSize(220, 320);
        kitchen.setStyle("-fx-border-color: #2563eb; -fx-border-width: 2; -fx-background-color: transparent;");
        floorGrid.add(kitchen, 3, 1, 2, 4);
    }

    private void addTableButton(char col, int row) {
        String id = "" + col + row;
        RestaurantTable table = AppContext.tables().requireTable(id);
        Button b = new Button(id);
        b.getStyleClass().addAll("table-btn");
        applyStatusStyle(b, table.status());
        table.statusProperty().addListener((obs, oldV, newV) -> applyStatusStyle(b, newV));
        b.setOnAction(e -> setSelected(table));

        int gridCol = (col - 'A') + 1;
        int gridRow = row;
        floorGrid.add(b, gridCol, gridRow);
        tableButtons.put(id, b);
    }

    private void setSelected(RestaurantTable table) {
        this.selectedTable = table;
        refreshSelectedPanel();
    }

    private void refreshClockUi() {
        boolean clockedIn = AppContext.session().isClockedIn();
        clockButton.setText(clockedIn ? "Clock Out" : "Clock In");
        elapsedLabel.setText(clockedIn ? "Elapsed: " + AppContext.clock().elapsedText() : "");
        if (clockLabel != null) {
            clockLabel.setText(ZonedDateTime.now(EASTERN).format(CLOCK_FMT));
        }
        refreshSelectedPanel();
    }

    private void refreshSelectedPanel() {
        if (selectedTable == null) {
            selectedTableLabel.setText("None");
            selectedStatusLabel.setText("");
            markOpenBtn.setDisable(true);
            return;
        }
        selectedTableLabel.setText(selectedTable.id());
        selectedStatusLabel.setText("Status: " + displayStatus(selectedTable.status()));

        boolean clockedIn = AppContext.session().isClockedIn();
        boolean dirty = selectedTable.status() == TableStatus.DIRTY;
        markOpenBtn.setDisable(!(clockedIn && dirty));
    }

    private void applyStatusStyle(Button b, TableStatus status) {
        b.getStyleClass().removeAll("status-open", "status-occupied", "status-dirty");
        if (status == TableStatus.OPEN) b.getStyleClass().add("status-open");
        if (status == TableStatus.OCCUPIED) b.getStyleClass().add("status-occupied");
        if (status == TableStatus.DIRTY) b.getStyleClass().add("status-dirty");
    }

    private static String displayStatus(TableStatus status) {
        if (status == null) return "";
        return switch (status) {
            case OPEN -> "READY";
            case OCCUPIED -> "OCCUPIED";
            case DIRTY -> "DIRTY";
        };
    }
}

