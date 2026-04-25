package com.jjcorner.app.controller.waiter;
import com.jjcorner.app.AppContext;
import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.MenuCategory;
import com.jjcorner.app.model.MenuItem;
import com.jjcorner.app.model.OrderStatus;
import com.jjcorner.app.model.RestaurantTable;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.model.TableStatus;
import com.jjcorner.app.model.Ticket;
import com.jjcorner.app.model.TicketItem;
import com.jjcorner.app.nav.SceneManager;
import com.jjcorner.app.service.OrderService;
import com.jjcorner.app.ui.checkout.CheckoutScreen;
import com.jjcorner.app.ui.checkout.ClosedCheckDialog;
import com.jjcorner.app.util.AlertHelper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public final class WaiterHomeController implements Initializable {
    @FXML private Label titleLabel;
    @FXML private Button clockButton;
    @FXML private Label elapsedLabel;
    @FXML private GridPane floorGrid;
    @FXML private Label selectedTableLabel;
    @FXML private Label selectedStatusLabel;
    @FXML private Label assignedLabel;
    @FXML private Button markOccupiedBtn;
    @FXML private Button markDirtyBtn;
    @FXML private Button openTicketBtn;
    @FXML private Button checkoutBtn;
    @FXML private Label hintLabel;
    @FXML private Label clockLabel;
    @FXML private ImageView logoImage;
    @FXML private Button closedCheckHistoryBtn;

    private final Map<String, Button> tableButtons = new HashMap<>();
    private RestaurantTable selectedTable;
    private Timeline elapsedTimer;
    private static final ZoneId EASTERN = ZoneId.of("America/New_York");
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Employee u = AppContext.session().currentUser();
        titleLabel.setText("Waiter - " + (u == null ? "" : u.displayName()));

        buildFloorGrid();
        setSelected(null);
        refreshClockUi();

        elapsedTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshClockUi()));
        elapsedTimer.setCycleCount(Timeline.INDEFINITE);
        elapsedTimer.play();

        hintLabel.setText("Tip: Clock in to enable table and order actions.");
        if (logoImage != null && logoImage.getImage() == null) {
            var url = getClass().getResource("/com/jjcorner/view/jj-logo.png");
            if (url != null) logoImage.setImage(new Image(url.toExternalForm()));
        }
        refreshShortcutClockState();
    }

    private void refreshShortcutClockState() {
        boolean clockedIn = AppContext.session().isClockedIn();
        if (closedCheckHistoryBtn != null) {
            closedCheckHistoryBtn.setDisable(!clockedIn);
        }
    }

    public void onHome() {
        setSelected(null);
    }

    public void onClockToggle() {
        if (!AppContext.session().isClockedIn()) {
            AppContext.clock().clockIn();
        } else {
            if (AppContext.orders().currentWaiterHasOpenChecks()) {
                AlertHelper.error("Open checks", "Error: You cannot clock out because you have open checks.");
                return;
            }
            Employee me = AppContext.session().currentUser();
            if (me != null && AppContext.tables().waiterHasOccupiedAssignedTable(me.employeeId())) {
                AlertHelper.error("Unable to log out", "Mark occupied tables as dirty.");
                return;
            }
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

    public void onViewMyTables() {
        Employee u = AppContext.session().currentUser();
        if (u == null) return;
        var tables = FXCollections.observableArrayList(AppContext.tables().waiterAssignedTables(u.employeeId()));
        ListView<RestaurantTable> lv = new ListView<>(tables);
        lv.setCellFactory(x -> new ListCell<>() {
            @Override
            protected void updateItem(RestaurantTable item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.id() + " - " + item.status());
                }
            }
        });
        lv.setOnMouseClicked(e -> {
            RestaurantTable t = lv.getSelectionModel().getSelectedItem();
            if (t != null) {
                setSelected(t);
            }
        });

        Stage s = simpleDialog("My Tables", lv, 360, 480);
        s.showAndWait();
    }

    public void onViewOrderStatus() {
        var tickets = AppContext.orders().ticketsForCurrentWaiter();
        ListView<Ticket> lv = new ListView<>(tickets);
        lv.setCellFactory(x -> new ListCell<>() {
            @Override
            protected void updateItem(Ticket item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.tableId() + " · Order #" + item.orderNumber() + " — " + item.status());
                    if (item.status() == OrderStatus.READY) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        lv.setOnMouseClicked(e -> {
            Ticket t = lv.getSelectionModel().getSelectedItem();
            if (t != null) {
                setSelected(AppContext.tables().requireTable(t.tableId()));
            }
        });

        Stage s = simpleDialog("Order Status", lv, 420, 520);
        s.showAndWait();
    }

    public void onMarkOccupied() {
        if (selectedTable == null) {
            AlertHelper.error("Select a table", "Error: Please select a table first");
            return;
        }
        Integer guests = promptGuestCount();
        if (guests == null) {
            return;
        }
        selectedTable.setGuestCount(guests);
        try {
            requireClockedIn();
            ensureAssigned(selectedTable);
            AppContext.tables().attemptStatusChange(Role.WAITER, selectedTable, TableStatus.OCCUPIED);
        } catch (RuntimeException ex) {
            AlertHelper.error("Status change", ex.getMessage());
        }
        refreshSelectedPanel();
    }

    public void onRetrieveClosedCheck() {
        try {
            requireClockedIn();
        } catch (IllegalStateException ex) {
            AlertHelper.error("Protected action", ex.getMessage());
            return;
        }
        var history = AppContext.orders().closedCheckHistoryForCurrentWaiter();
        if (history.isEmpty()) {
            AlertHelper.info("Closed check history", "You have no closed checks in history yet.");
            return;
        }
        ListView<Ticket> lv = new ListView<>(FXCollections.observableArrayList(history));
        lv.setCellFactory(x -> new ListCell<>() {
            @Override
            protected void updateItem(Ticket item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String refunds = OrderService.ticketHasRefunds(item) ? " · Refunds" : "";
                    setText("Table " + item.tableId() + " · Order #" + item.orderNumber()
                            + " · " + item.guestCount() + " guests · " + item.status() + refunds);
                }
            }
        });

        Button openBtn = new Button("View check");
        openBtn.getStyleClass().add("btn-primary");
        openBtn.setDefaultButton(true);
        VBox content = new VBox(10,
                new Label("Select a check from history (includes partial refunds):"),
                lv
        );
        VBox.setVgrow(lv, Priority.ALWAYS);
        content.setPadding(new Insets(12));

        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Closed check history");
        Window owner = markOccupiedBtn.getScene() != null ? markOccupiedBtn.getScene().getWindow() : null;
        if (owner != null) {
            dlg.initOwner(owner);
        }
        openBtn.setOnAction(e -> {
            Ticket t = lv.getSelectionModel().getSelectedItem();
            if (t == null) {
                AlertHelper.error("Closed check", "Select a check from the list.");
                return;
            }
            ClosedCheckDialog.show(dlg, t);
        });
        Button closeDlg = new Button("Close");
        closeDlg.setCancelButton(true);
        closeDlg.setOnAction(e -> dlg.close());
        HBox actions = new HBox(10, openBtn, closeDlg);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox root = new VBox(12, content, actions);
        root.setPadding(new Insets(8));
        Scene sc = new Scene(root, 440, 420);
        var css = getClass().getResource("/com/jjcorner/view/styles.css");
        if (css != null) {
            sc.getStylesheets().add(css.toExternalForm());
        }
        dlg.setScene(sc);
        dlg.showAndWait();
    }

    public void onMarkDirty() {
        if (selectedTable == null) {
            AlertHelper.error("Select a table", "Error: Please select a table first");
            return;
        }
        try {
            requireClockedIn();
            ensureAssigned(selectedTable);
            // Enforce payment when a ticket still owes; allow dirty if no ticket or latest is paid (or empty draft)
            Ticket t = AppContext.orders().latestTicketForTable(selectedTable.id()).orElse(null);
            boolean blocking = t != null
                    && t.status() != OrderStatus.PAID
                    && !(t.status() == OrderStatus.DRAFT && t.items().isEmpty());
            if (blocking) {
                AlertHelper.error("Checkout required", "Error: Please complete checkout/payment before marking table dirty.");
                return;
            }
            AppContext.tables().attemptStatusChange(Role.WAITER, selectedTable, TableStatus.DIRTY);
        } catch (IllegalArgumentException ex) {
            AlertHelper.error("Status change", ex.getMessage());
        } catch (IllegalStateException ex) {
            AlertHelper.error("Protected action", ex.getMessage());
        }
        refreshSelectedPanel();
    }

    public void onOpenTicket() {
        if (selectedTable == null) {
            AlertHelper.error("Select a table", "Error: Please select a table first");
            return;
        }
        try {
            requireClockedIn();
            ensureAssigned(selectedTable);
            Ticket ticket = AppContext.orders().openOrCreateForTable(selectedTable.id());
            openTicketDialog(ticket);
        } catch (RuntimeException ex) {
            AlertHelper.error("Order", ex.getMessage());
        }
        refreshSelectedPanel();
    }

    public void onCheckout() {
        if (selectedTable == null) {
            AlertHelper.error("Select a table", "Error: Please select a table first");
            return;
        }
        try {
            requireClockedIn();
            ensureAssigned(selectedTable);
            Ticket ticket = AppContext.orders().openOrCreateForTable(selectedTable.id());
            openCheckoutDialog(ticket);
        } catch (RuntimeException ex) {
            AlertHelper.error("Checkout", ex.getMessage());
        }
        refreshSelectedPanel();
    }

    private void buildFloorGrid() {
        floorGrid.getChildren().clear();
        tableButtons.clear();
        floorGrid.setHgap(18);
        floorGrid.setVgap(16);

        // Grid coordinates:
        // col 0 = row labels
        // col 1..6 = A..F
        // row 0 = spacer, rows 1..6 = table rows, row 7 = column labels
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

        // Place 28 tables to match diagram:
        // Left block A,B rows 1-6
        // Right block E,F rows 1-6
        // Bottom middle C,D rows 5-6
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

        // Center "kitchen" block visual
        VBox kitchen = new VBox();
        kitchen.setMinSize(220, 320);
        kitchen.setStyle("-fx-border-color: #2563eb; -fx-border-width: 2; -fx-background-color: transparent;");
        floorGrid.add(kitchen, 3, 1, 2, 4); // spans C..D, rows 1..4
    }

    private void addTableButton(char col, int row) {
        String id = "" + col + row;
        RestaurantTable table = AppContext.tables().requireTable(id);
        Button b = new Button(id);
        b.getStyleClass().addAll("table-btn");
        applyStatusStyle(b, table.status());
        table.statusProperty().addListener((obs, oldV, newV) -> applyStatusStyle(b, newV));
        b.setOnAction(e -> onTableClicked(table));

        int gridCol = (col - 'A') + 1;
        int gridRow = row;
        floorGrid.add(b, gridCol, gridRow);
        tableButtons.put(id, b);
    }

    private void onTableClicked(RestaurantTable table) {
        if (!AppContext.session().isClockedIn()) {
            AlertHelper.error("Protected action", "Error: You are not clocked in");
            return;
        }
        setSelected(table);
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
        refreshShortcutClockState();
        Employee u = AppContext.session().currentUser();
        if (selectedTable == null) {
            selectedTableLabel.setText("None");
            selectedStatusLabel.setText("");
            assignedLabel.setText("");
            markOccupiedBtn.setDisable(true);
            markDirtyBtn.setDisable(true);
            openTicketBtn.setDisable(true);
            checkoutBtn.setDisable(true);
            return;
        }

        selectedTableLabel.setText(selectedTable.id());
        selectedStatusLabel.setText("Status: " + displayStatus(selectedTable.status()));

        boolean assignedToMe = u != null && selectedTable.isAssignedTo(u.employeeId());
        assignedLabel.setText(assignedToMe ? "Assigned to you" : "Not assigned to you");

        boolean clockedIn = AppContext.session().isClockedIn();
        boolean canTouch = clockedIn && assignedToMe;

        markOccupiedBtn.setDisable(!(canTouch && selectedTable.status() == TableStatus.OPEN));

        // markDirty only after payment; we allow user to go to checkout first
        markDirtyBtn.setDisable(!(canTouch && selectedTable.status() == TableStatus.OCCUPIED));

        openTicketBtn.setDisable(!(canTouch && selectedTable.status() == TableStatus.OCCUPIED));
        checkoutBtn.setDisable(!(canTouch && selectedTable.status() == TableStatus.OCCUPIED));
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

    private void requireClockedIn() {
        if (!AppContext.session().isClockedIn()) {
            throw new IllegalStateException("Error: You are not clocked in");
        }
    }

    private void ensureAssigned(RestaurantTable table) {
        Employee u = AppContext.session().currentUser();
        if (u == null) throw new IllegalStateException("Not logged in");
        if (!table.isAssignedTo(u.employeeId())) {
            throw new IllegalArgumentException("Error: Table is not assigned to you");
        }
    }

    private Stage simpleDialog(String title, javafx.scene.Node content, int w, int h) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle(title);
        VBox root = new VBox(content);
        root.setPadding(new Insets(12));
        s.setScene(new Scene(root, w, h));
        return s;
    }

    private Integer promptGuestCount() {
        Spinner<Integer> sp = new Spinner<>(1, 4, 2);
        sp.setEditable(false);
        VBox content = new VBox(10,
                new Label("How many guests at this table?"),
                labeled("Guests", sp)
        );
        content.setPadding(new Insets(14));
        final Integer[] result = {null};
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Party size");
        Window win = markOccupiedBtn.getScene() != null ? markOccupiedBtn.getScene().getWindow() : null;
        if (win != null) {
            s.initOwner(win);
        }
        Button ok = new Button("OK");
        ok.getStyleClass().add("btn-primary");
        Button cancel = new Button("Cancel");
        ok.setOnAction(e -> {
            result[0] = sp.getValue();
            s.close();
        });
        cancel.setOnAction(e -> s.close());
        HBox actions = new HBox(10, ok, cancel);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox root = new VBox(12, content, actions);
        root.setPadding(new Insets(12));
        Scene scene = new Scene(root, 340, 200);
        var css = getClass().getResource("/com/jjcorner/view/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        s.setScene(scene);
        s.showAndWait();
        return result[0];
    }

    private void openTicketDialog(Ticket ticket) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Order #" + ticket.orderNumber() + " · Table " + ticket.tableId());

        ChoiceBox<MenuCategory> categoryBox = new ChoiceBox<>(FXCollections.observableArrayList(MenuCategory.values()));
        categoryBox.getSelectionModel().select(MenuCategory.APPETIZERS);

        ListView<MenuItem> menuList = new ListView<>();
        menuList.setCellFactory(x -> new ListCell<>() {
            @Override
            protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });

        TextArea itemDetails = new TextArea();
        itemDetails.setEditable(false);
        itemDetails.setWrapText(true);
        itemDetails.setPromptText("Select a menu item to see details");
        itemDetails.setPrefRowCount(4);

        menuList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                itemDetails.clear();
            } else {
                String desc = newV.description();
                itemDetails.setText(desc == null || desc.isBlank() ? "(No description)" : desc);
            }
        });

        Runnable refreshMenu = () -> menuList.setItems(FXCollections.observableArrayList(
                AppContext.menu().byCategory(categoryBox.getValue())
        ));
        categoryBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshMenu.run());
        refreshMenu.run();

        Spinner<Integer> seatSpinner = new Spinner<>(1, 4, 1);
        Spinner<Integer> qtySpinner = new Spinner<>(1, 20, 1);
        TextArea notes = new TextArea();
        notes.setPromptText("Special notes / modifiers (optional)");
        notes.setPrefRowCount(2);

        Button addBtn = new Button("Add Item");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> {
            MenuItem item = menuList.getSelectionModel().getSelectedItem();
            if (item == null) {
                AlertHelper.error("Add item", "Select a menu item first.");
                return;
            }
            try {
                String extra = "";
                BigDecimal unitExtra = BigDecimal.ZERO;
                if (item.category() == MenuCategory.ENTREES) {
                    extra = promptEntreeChoices(item);
                    if (extra == null) return;
                } else {
                    LineMods mods = promptNonEntreeLineMods(item);
                    if (mods == null) return;
                    extra = mods.notes();
                    unitExtra = mods.unitExtra();
                }

                String combinedNotes = notes.getText();
                if (combinedNotes == null) combinedNotes = "";
                if (!extra.isBlank()) {
                    combinedNotes = combinedNotes.isBlank() ? extra : (combinedNotes + "\n" + extra);
                }

                AppContext.orders().addItem(ticket, item, seatSpinner.getValue(), qtySpinner.getValue(), combinedNotes, unitExtra);
                notes.clear();
                qtySpinner.getValueFactory().setValue(1);
            } catch (RuntimeException ex) {
                AlertHelper.error("Add item", ex.getMessage());
            }
        });

        TableView<TicketItem> itemsTable = new TableView<>(ticket.items());
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<TicketItem, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().menuItem().name()));
        TableColumn<TicketItem, Integer> seatCol = new TableColumn<>("Seat");
        seatCol.setCellValueFactory(new PropertyValueFactory<>("seat"));
        TableColumn<TicketItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<TicketItem, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        TableColumn<TicketItem, String> rfdCol = new TableColumn<>("Refund");
        rfdCol.setMaxWidth(90);
        rfdCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> {
            int r = cd.getValue().refundedQuantity();
            return r <= 0 ? "—" : "Refunded" + (r > 1 ? " (" + r + ")" : "");
        }, cd.getValue().refundedQuantityProperty()));
        itemsTable.getColumns().addAll(itemCol, seatCol, qtyCol, notesCol, rfdCol);

        Button removeBtn = new Button("Remove Selected");
        removeBtn.setOnAction(e -> {
            TicketItem sel = itemsTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                AlertHelper.error("Remove item", "Select an item in the ticket first.");
                return;
            }
            try {
                AppContext.orders().removeItem(ticket, sel);
            } catch (RuntimeException ex) {
                AlertHelper.error("Remove item", ex.getMessage());
            }
        });

        Button submitBtn = new Button("Submit to Kitchen");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setOnAction(e -> {
            try {
                AppContext.orders().submitToKitchen(ticket);
                if (AppContext.session().isDemoMode()) {
                    AlertHelper.info("Demo mode", "Practice mode: ticket was not sent to kitchen.");
                } else {
                    AlertHelper.info("Submitted", "Ticket submitted to kitchen.");
                }
            } catch (RuntimeException ex) {
                AlertHelper.error("Submit", ex.getMessage());
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> s.close());

        Label status = new Label();
        status.textProperty().bind(ticket.statusProperty().asString("Status: %s"));

        Runnable refreshEditable = () -> {
            boolean paid = ticket.status() == OrderStatus.PAID;
            boolean hasUnsent = ticket.items().stream().anyMatch(i -> !i.isSentToKitchen());
            addBtn.setDisable(paid);
            submitBtn.setDisable(paid || !hasUnsent);
            TicketItem sel = itemsTable.getSelectionModel().getSelectedItem();
            boolean canRemove = !paid && sel != null && !sel.isSentToKitchen();
            removeBtn.setDisable(!canRemove);
        };
        ticket.statusProperty().addListener((obs, o, n) -> refreshEditable.run());
        ticket.items().addListener((ListChangeListener<TicketItem>) ch -> {
            while (ch.next()) {
                for (TicketItem it : ch.getAddedSubList()) {
                    it.sentToKitchenProperty().addListener((x, a, b) -> refreshEditable.run());
                }
            }
            refreshEditable.run();
        });
        ticket.items().forEach(it ->
                it.sentToKitchenProperty().addListener((x, a, b) -> refreshEditable.run()));
        itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshEditable.run());
        refreshEditable.run();

        VBox left = new VBox(10,
                new Label("Category"), categoryBox,
                new Label("Menu Items"), menuList,
                new Label("Item Details"), itemDetails
        );
        left.setPrefWidth(320);

        HBox seatQty = new HBox(10,
                labeled("Seat", seatSpinner),
                labeled("Quantity", qtySpinner)
        );

        Label orderMeta = new Label("Order #" + ticket.orderNumber() + " · " + ticket.guestCount() + " guests");
        orderMeta.getStyleClass().add("muted");

        VBox right = new VBox(10,
                orderMeta,
                status,
                seatQty,
                notes,
                addBtn,
                new Label("Ticket Items"),
                itemsTable,
                new HBox(10, removeBtn, submitBtn, closeBtn)
        );
        VBox.setVgrow(itemsTable, Priority.ALWAYS);

        HBox center = new HBox(14, left, right);
        center.setPadding(new Insets(12));
        center.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(right, Priority.ALWAYS);

        Button homeBtn = new Button("⌂ Home");
        homeBtn.setOnAction(e -> s.close());
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(12, jjLogoSmall(), topSpacer, homeBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 14, 10, 14));
        topBar.getStyleClass().add("topbar");

        BorderPane bp = new BorderPane();
        bp.setTop(topBar);
        bp.setCenter(center);

        Scene scene = new Scene(bp, 980, 640);
        scene.getStylesheets().add(getClass().getResource("/com/jjcorner/view/styles.css").toExternalForm());
        s.setScene(scene);
        s.showAndWait();
    }

    private ImageView jjLogoSmall() {
        var stream = getClass().getResourceAsStream("/com/jjcorner/view/jj-logo.png");
        ImageView iv = new ImageView(stream != null ? new Image(stream) : null);
        iv.setFitHeight(44);
        iv.setFitWidth(44);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }

    private record LineMods(String notes, BigDecimal unitExtra) {
        static LineMods none() {
            return new LineMods("", BigDecimal.ZERO);
        }
    }

    private String promptEntreeChoices(MenuItem item) {
        var sides = FXCollections.observableArrayList(
                "Curly Fries",
                "Wing Chips",
                "Sweet Potato Fries",
                "Creamy Cabbage Slaw",
                "Adluh Cheese Grits",
                "Mashed Potatoes",
                "Mac & Cheese",
                "Seasonal Vegetables",
                "Baked Beans"
        );

        boolean isMacBar = "ENT-009".equalsIgnoreCase(item.id()) || item.name().toLowerCase().contains("mac & cheese bar");
        boolean isStripSteak = "ENT-005".equalsIgnoreCase(item.id());

        ChoiceBox<String> side1 = new ChoiceBox<>(sides);
        ChoiceBox<String> side2 = new ChoiceBox<>(sides);
        side1.getSelectionModel().selectFirst();
        side2.getSelectionModel().select(1);

        VBox content = new VBox(10);
        content.setPadding(new Insets(12));
        content.getChildren().addAll(
                new Label("Entree requires 2 sides:"),
                labeled("Side 1", side1),
                labeled("Side 2", side2)
        );

        ChoiceBox<String> tempChoice = null;
        if (isStripSteak) {
            tempChoice = new ChoiceBox<>(FXCollections.observableArrayList(
                    "Well Done", "Medium Well", "Medium", "Medium Rare", "Rare", "Blue"
            ));
            tempChoice.getSelectionModel().select(2);
            content.getChildren().addAll(
                    new Label("Steak temperature:"),
                    labeled("Temperature", tempChoice)
            );
        }

        final ChoiceBox<String> top1;
        final ChoiceBox<String> top2;
        final ChoiceBox<String> cheeseStyle;
        if (isMacBar) {
            var toppings = FXCollections.observableArrayList(
                    "Pepper Jack Cheese",
                    "Cheddar Cheese",
                    "Swiss Cheese",
                    "Mozzarella Cheese",
                    "Goat Cheese",
                    "Bacon",
                    "Broccoli",
                    "Mushrooms",
                    "Grilled Onions",
                    "Jalapenos",
                    "Spinach",
                    "Tomatoes"
            );
            top1 = new ChoiceBox<>(toppings);
            top2 = new ChoiceBox<>(toppings);
            top1.getSelectionModel().selectFirst();
            top2.getSelectionModel().select(1);

            cheeseStyle = new ChoiceBox<>(FXCollections.observableArrayList("Regular cheese", "Spicy cheese"));
            cheeseStyle.getSelectionModel().selectFirst();

            content.getChildren().addAll(
                    new Label("Mac & Cheese Bar requires 2 toppings:"),
                    labeled("Topping 1", top1),
                    labeled("Topping 2", top2),
                    new Label("Cheese base:"),
                    labeled("Mac & cheese style", cheeseStyle)
            );
        } else {
            top1 = null;
            top2 = null;
            cheeseStyle = null;
        }

        final ChoiceBox<String> tempChoiceFinal = tempChoice;
        final ChoiceBox<String> top1Final = top1;
        final ChoiceBox<String> top2Final = top2;
        final ChoiceBox<String> cheeseStyleFinal = cheeseStyle;

        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Entree Options");

        Button ok = new Button("OK");
        ok.getStyleClass().add("btn-primary");
        Button cancel = new Button("Cancel");

        final boolean[] accepted = {false};
        ok.setOnAction(e -> {
            if (side1.getValue() == null || side2.getValue() == null) {
                AlertHelper.error("Sides required", "Select two sides.");
                return;
            }
            if (side1.getValue().equals(side2.getValue())) {
                AlertHelper.error("Sides required", "Sides must be two different choices.");
                return;
            }
            if (isStripSteak && tempChoiceFinal != null && tempChoiceFinal.getValue() == null) {
                AlertHelper.error("Temperature required", "Select a steak temperature.");
                return;
            }
            if (isMacBar) {
                if (top1Final == null || top2Final == null || top1Final.getValue() == null || top2Final.getValue() == null) {
                    AlertHelper.error("Toppings required", "Select two toppings.");
                    return;
                }
                if (top1Final.getValue().equals(top2Final.getValue())) {
                    AlertHelper.error("Toppings required", "Toppings must be two different choices.");
                    return;
                }
                if (cheeseStyleFinal == null || cheeseStyleFinal.getValue() == null) {
                    AlertHelper.error("Cheese style required", "Select regular or spicy cheese.");
                    return;
                }
            }
            accepted[0] = true;
            s.close();
        });
        cancel.setOnAction(e -> s.close());

        HBox actions = new HBox(10, ok, cancel);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, content, actions);
        root.setPadding(new Insets(12));

        int prefH = 260;
        if (isMacBar) prefH = 520;
        else if (isStripSteak) prefH = 360;

        Scene scene = new Scene(root, 440, prefH);
        scene.getStylesheets().add(getClass().getResource("/com/jjcorner/view/styles.css").toExternalForm());
        s.setScene(scene);
        s.showAndWait();

        if (!accepted[0]) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("Sides: ").append(side1.getValue()).append(", ").append(side2.getValue());
        if (isStripSteak && tempChoiceFinal != null) {
            sb.append("\nTemperature: ").append(tempChoiceFinal.getValue());
        }
        if (isMacBar) {
            sb.append("\nToppings: ").append(top1Final.getValue()).append(", ").append(top2Final.getValue());
            sb.append("\nCheese style: ").append(cheeseStyleFinal.getValue());
        }
        return sb.toString();
    }

    private LineMods promptNonEntreeLineMods(MenuItem item) {
        return switch (item.id()) {
            case "APP-001", "APP-002" -> promptNachosBbq();
            case "APP-003" -> promptSliders();
            case "APP-005" -> promptFriedVeggies();
            case "SID-007" -> promptSideMacCheese();
            case "SND-005" -> promptMeatballSub();
            case "BRG-001" -> promptBaconCheeseburger();
            case "BRG-002" -> promptCarolinaBurger();
            default -> LineMods.none();
        };
    }

    private LineMods promptNachosBbq() {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("BBQ sauce");
        Label msg = new Label("Add BBQ sauce for $0.50 extra?");
        final boolean[] result = {false};
        final boolean[] cancelled = {true};
        Button yes = new Button("Yes (+$0.50)");
        yes.getStyleClass().add("btn-primary");
        Button no = new Button("No");
        Button cancel = new Button("Cancel");
        yes.setOnAction(e -> {
            result[0] = true;
            cancelled[0] = false;
            s.close();
        });
        no.setOnAction(e -> {
            result[0] = false;
            cancelled[0] = false;
            s.close();
        });
        cancel.setOnAction(e -> s.close());
        VBox root = new VBox(12, msg, new HBox(10, yes, no, cancel));
        root.setPadding(new Insets(14));
        Scene scene = new Scene(root, 380, 140);
        scene.getStylesheets().add(getClass().getResource("/com/jjcorner/view/styles.css").toExternalForm());
        s.setScene(scene);
        s.showAndWait();
        if (cancelled[0]) return null;
        if (result[0]) {
            return new LineMods("Add BBQ sauce (+$0.50)", new BigDecimal("0.50"));
        }
        return LineMods.none();
    }

    private LineMods promptSliders() {
        ChoiceBox<String> meat = new ChoiceBox<>(FXCollections.observableArrayList("Chicken", "Pork"));
        meat.getSelectionModel().selectFirst();
        ChoiceBox<String> sauce = new ChoiceBox<>(FXCollections.observableArrayList(
                "Chipotle", "Jim Beam", "Carolina Gold BBQ", "No sauce"
        ));
        sauce.getSelectionModel().selectFirst();
        VBox content = new VBox(10,
                new Label("Sliders — choose protein and sauce:"),
                labeled("Protein", meat),
                labeled("Sauce", sauce)
        );
        content.setPadding(new Insets(12));
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Slider options");
        final boolean[] ok = {false};
        Button confirm = new Button("OK");
        confirm.getStyleClass().add("btn-primary");
        Button cancel = new Button("Cancel");
        confirm.setOnAction(e -> {
            if (meat.getValue() == null || sauce.getValue() == null) {
                AlertHelper.error("Sliders", "Select protein and sauce.");
                return;
            }
            ok[0] = true;
            s.close();
        });
        cancel.setOnAction(e -> s.close());
        VBox root = new VBox(12, content, new HBox(10, confirm, cancel));
        root.setPadding(new Insets(12));
        Scene scene = new Scene(root, 400, 220);
        scene.getStylesheets().add(getClass().getResource("/com/jjcorner/view/styles.css").toExternalForm());
        s.setScene(scene);
        s.showAndWait();
        if (!ok[0]) return null;
        String n = "Protein: " + meat.getValue() + "\nSauce: " + sauce.getValue();
        return new LineMods(n, BigDecimal.ZERO);
    }

    private LineMods promptFriedVeggies() {
        ChoiceBox<String> veg = new ChoiceBox<>(FXCollections.observableArrayList(
                "Okra", "Zucchini", "Squash", "Mix & Match"
        ));
        veg.getSelectionModel().selectFirst();
        Label heading = new Label("Fried veggies — choose one: Okra, Zucchini, Squash or Mix & Match");
        heading.setWrapText(true);
        VBox content = new VBox(10, heading, labeled("Selection", veg));
        content.setPadding(new Insets(12));
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Fried veggies");
        final boolean[] ok = {false};
        Button confirm = new Button("OK");
        confirm.getStyleClass().add("btn-primary");
        Button cancel = new Button("Cancel");
        confirm.setOnAction(e -> {
            if (veg.getValue() == null) {
                AlertHelper.error("Fried veggies", "Make a selection.");
                return;
            }
            ok[0] = true;
            s.close();
        });
        cancel.setOnAction(e -> s.close());
        VBox root = new VBox(12, content, new HBox(10, confirm, cancel));
        root.setPadding(new Insets(12));
        Scene scene = new Scene(root, 360, 160);
        scene.getStylesheets().add(getClass().getResource("/com/jjcorner/view/styles.css").toExternalForm());
        s.setScene(scene);
        s.showAndWait();
        if (!ok[0]) return null;
        return new LineMods("Vegetable: " + veg.getValue(), BigDecimal.ZERO);
    }

    private LineMods promptSideMacCheese() {
        ChoiceBox<String> style = new ChoiceBox<>(FXCollections.observableArrayList("Regular cheese", "Spicy cheese"));
        style.getSelectionModel().selectFirst();
        VBox content = new VBox(10, new Label("Mac & Cheese — choose cheese style:"), labeled("Style", style));
        content.setPadding(new Insets(12));
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Mac & Cheese");
        final boolean[] ok = {false};
        Button confirm = new Button("OK");
        confirm.getStyleClass().add("btn-primary");
        Button cancel = new Button("Cancel");
        confirm.setOnAction(e -> {
            if (style.getValue() == null) {
                AlertHelper.error("Mac & Cheese", "Select a style.");
                return;
            }
            ok[0] = true;
            s.close();
        });
        cancel.setOnAction(e -> s.close());
        VBox root = new VBox(12, content, new HBox(10, confirm, cancel));
        root.setPadding(new Insets(12));
        Scene scene = new Scene(root, 380, 150);
        scene.getStylesheets().add(getClass().getResource("/com/jjcorner/view/styles.css").toExternalForm());
        s.setScene(scene);
        s.showAndWait();
        if (!ok[0]) return null;
        return new LineMods("Cheese style: " + style.getValue(), BigDecimal.ZERO);
    }

    private LineMods promptMeatballSub() {
        ChoiceBox<String> peppers = new ChoiceBox<>(FXCollections.observableArrayList(
                "Yes — sautéed peppers & onions", "No"
        ));
        peppers.getSelectionModel().selectFirst();
        VBox content = new VBox(10,
                new Label("Meatball sub — sautéed peppers and onions?"),
                labeled("Choice", peppers)
        );
        content.setPadding(new Insets(12));
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Meatball sub");
        final boolean[] ok = {false};
        Button confirm = new Button("OK");
        confirm.getStyleClass().add("btn-primary");
        Button cancel = new Button("Cancel");
        confirm.setOnAction(e -> {
            if (peppers.getValue() == null) {
                AlertHelper.error("Meatball sub", "Select yes or no.");
                return;
            }
            ok[0] = true;
            s.close();
        });
        cancel.setOnAction(e -> s.close());
        VBox root = new VBox(12, content, new HBox(10, confirm, cancel));
        root.setPadding(new Insets(12));
        Scene scene = new Scene(root, 420, 160);
        scene.getStylesheets().add(getClass().getResource("/com/jjcorner/view/styles.css").toExternalForm());
        s.setScene(scene);
        s.showAndWait();
        if (!ok[0]) return null;
        return new LineMods("Peppers & onions: " + peppers.getValue(), BigDecimal.ZERO);
    }

    private static ChoiceBox<String> steakTempChoiceBox() {
        ChoiceBox<String> t = new ChoiceBox<>(FXCollections.observableArrayList(
                "Well Done", "Medium Well", "Medium", "Medium Rare", "Rare", "Blue"
        ));
        t.getSelectionModel().select(2);
        return t;
    }

    private LineMods promptBaconCheeseburger() {
        ChoiceBox<String> cheese = new ChoiceBox<>(FXCollections.observableArrayList(
                "American", "Swiss", "Provolone", "Pepper Jack", "Blue Cheese", "Pimiento Cheese"
        ));
        cheese.getSelectionModel().selectFirst();
        ChoiceBox<String> temp = steakTempChoiceBox();
        VBox content = new VBox(10,
                new Label("Bacon cheeseburger options:"),
                labeled("Cheese", cheese),
                labeled("Temperature", temp)
        );
        content.setPadding(new Insets(12));
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Burger options");
        final boolean[] ok = {false};
        Button confirm = new Button("OK");
        confirm.getStyleClass().add("btn-primary");
        Button cancel = new Button("Cancel");
        confirm.setOnAction(e -> {
            if (cheese.getValue() == null || temp.getValue() == null) {
                AlertHelper.error("Burger", "Select cheese and temperature.");
                return;
            }
            ok[0] = true;
            s.close();
        });
        cancel.setOnAction(e -> s.close());
        VBox root = new VBox(12, content, new HBox(10, confirm, cancel));
        root.setPadding(new Insets(12));
        Scene scene = new Scene(root, 400, 220);
        scene.getStylesheets().add(getClass().getResource("/com/jjcorner/view/styles.css").toExternalForm());
        s.setScene(scene);
        s.showAndWait();
        if (!ok[0]) return null;
        String n = "Cheese: " + cheese.getValue() + "\nTemperature: " + temp.getValue();
        return new LineMods(n, BigDecimal.ZERO);
    }

    private LineMods promptCarolinaBurger() {
        ChoiceBox<String> temp = steakTempChoiceBox();
        VBox content = new VBox(10,
                new Label("Carolina burger — temperature:"),
                labeled("Temperature", temp)
        );
        content.setPadding(new Insets(12));
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Burger options");
        final boolean[] ok = {false};
        Button confirm = new Button("OK");
        confirm.getStyleClass().add("btn-primary");
        Button cancel = new Button("Cancel");
        confirm.setOnAction(e -> {
            if (temp.getValue() == null) {
                AlertHelper.error("Burger", "Select temperature.");
                return;
            }
            ok[0] = true;
            s.close();
        });
        cancel.setOnAction(e -> s.close());
        VBox root = new VBox(12, content, new HBox(10, confirm, cancel));
        root.setPadding(new Insets(12));
        Scene scene = new Scene(root, 380, 160);
        scene.getStylesheets().add(getClass().getResource("/com/jjcorner/view/styles.css").toExternalForm());
        s.setScene(scene);
        s.showAndWait();
        if (!ok[0]) return null;
        return new LineMods("Temperature: " + temp.getValue(), BigDecimal.ZERO);
    }

    private void openCheckoutDialog(Ticket ticket) {
        CheckoutScreen.show(ticket);
    }

    private static VBox labeled(String label, javafx.scene.Node node) {
        VBox v = new VBox(4, new Label(label), node);
        return v;
    }
}

