package com.jjcorner.app.controller.manager;

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
import com.jjcorner.app.util.HomeScreen;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Manager floor screen and administration area. Managers can perform waiter and busboy
 * actions, plus employee, menu, inventory, layout, and reporting tasks.
 */
public final class ManagerHomeController implements Initializable {
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
        titleLabel.setText("Manager - " + (u == null ? "" : u.displayName()));

        buildFloorGrid();
        setSelected(null);
        refreshClockUi();

        elapsedTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshClockUi()));
        elapsedTimer.setCycleCount(Timeline.INDEFINITE);
        elapsedTimer.play();

        hintLabel.setText("Manager actions are available without clocking in.");
        if (logoImage != null && logoImage.getImage() == null) {
            var url = getClass().getResource("/com/jjcorner/view/jj-logo.png");
            if (url != null) logoImage.setImage(new Image(url.toExternalForm()));
        }
        clockButton.setText("Manager");
        clockButton.setDisable(true);
        elapsedLabel.setText("");
        refreshShortcutClockState();
    }

    private void refreshShortcutClockState() {
        if (closedCheckHistoryBtn != null) {
            closedCheckHistoryBtn.setDisable(false);
        }
    }

    public void onHome() {
        HomeScreen.show(clockButton.getScene() == null ? null : clockButton.getScene().getWindow());
    }

    public void onClockToggle() {
        // Managers do not use the time-clock workflow.
    }

    public void onLogout() {
        SceneManager.logout();
    }

    public void onSwitchUser() {
        SceneManager.switchUser();
    }

    public void onViewMyTables() {
        Employee u = AppContext.session().currentUser();
        if (u == null) {
            return;
        }
        var tables = FXCollections.observableArrayList(AppContext.tables().allTables());
        ListView<RestaurantTable> lv = new ListView<>(tables);
        lv.setCellFactory(x -> new ListCell<>() {
            @Override
            protected void updateItem(RestaurantTable item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.assignedWaiterId() == null) {
                    setText(item.id() + " - " + item.status());
                } else {
                    setText(item.id() + " - " + item.status() + " - " + item.assignedWaiterId());
                }
            }
        });
        lv.setOnMouseClicked(e -> {
            RestaurantTable t = lv.getSelectionModel().getSelectedItem();
            if (t != null) {
                setSelected(t);
            }
        });

        Stage s = simpleDialog("All Tables", lv, 360, 480);
        s.showAndWait();
    }

    public void onViewOrderStatus() {
        var tickets = AppContext.orders().allTickets();
        ListView<Ticket> lv = new ListView<>(tickets);
        lv.setCellFactory(x -> new ListCell<>() {
            @Override
            protected void updateItem(Ticket item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.tableId() + " · Order #" + item.orderNumber() + " — " + item.status() +" - " +item.waiterId());
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

    public void onTableLayoutBtn() {
        TableView<RestaurantTable> table = new TableView<>(AppContext.tables().allTables());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<RestaurantTable, String> idCol = new TableColumn<>("Table");
        idCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().id()));
        TableColumn<RestaurantTable, String> seatsCol = new TableColumn<>("Seats");
        seatsCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> String.valueOf(cd.getValue().seatCount())));
        TableColumn<RestaurantTable, String> waiterCol = new TableColumn<>("Waiter");
        waiterCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> waiterName(cd.getValue().assignedWaiterId())));
        TableColumn<RestaurantTable, String> joinedCol = new TableColumn<>("Joined");
        joinedCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().joinedTableIds()));
        table.getColumns().addAll(idCol, seatsCol, waiterCol, joinedCol);

        TextField newTableId = new TextField();
        newTableId.setPromptText("G1");
        Spinner<Integer> seats = new Spinner<>(1, 24, 4);
        seats.setEditable(true);
        ComboBox<Employee> waiter = new ComboBox<>(FXCollections.observableArrayList(
                AppContext.auth().allEmployees().stream().filter(e -> e.role() == Role.WAITER).toList()
        ));
        waiter.setCellFactory(x -> employeeCell());
        waiter.setButtonCell(employeeCell());
        TextField joined = new TextField();
        joined.setPromptText("A1,A2");

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected == null) return;
            newTableId.setText(selected.id());
            seats.getValueFactory().setValue(selected.seatCount());
            joined.setText(selected.joinedTableIds());
            waiter.getSelectionModel().select(AppContext.auth().allEmployees().stream()
                    .filter(e -> e.employeeId().equalsIgnoreCase(selected.assignedWaiterId()))
                    .findFirst().orElse(null));
        });

        Button update = new Button("Update Selected");
        update.getStyleClass().add("btn-primary");
        update.setOnAction(e -> {
            RestaurantTable selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertHelper.error("Table Layout", "Select a table.");
                return;
            }
            Employee assigned = waiter.getValue();
            AppContext.tables().updateTableLayout(selected, assigned == null ? null : assigned.employeeId(), seats.getValue(), joined.getText());
            AppContext.activity().record(AppContext.session().currentUser(), "Updated layout for table " + selected.id());
            table.refresh();
            buildFloorGrid();
            refreshSelectedPanel();
        });

        Button add = new Button("Add Table");
        add.setOnAction(e -> {
            try {
                Employee assigned = waiter.getValue();
                AppContext.tables().addTable(newTableId.getText(), seats.getValue(), assigned == null ? null : assigned.employeeId());
                AppContext.activity().record(AppContext.session().currentUser(), "Added table " + newTableId.getText());
                table.refresh();
                buildFloorGrid();
            } catch (RuntimeException ex) {
                AlertHelper.error("Table Layout", ex.getMessage());
            }
        });

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("Table ID"), newTableId, new Label("Seats"), seats);
        form.addRow(1, new Label("Waiter"), waiter, new Label("Joined tables"), joined);
        form.add(new HBox(10, update, add), 1, 2, 3, 1);

        VBox root = new VBox(12, table, form);
        root.setPadding(new Insets(12));
        VBox.setVgrow(table, Priority.ALWAYS);
        Stage s = simpleDialog("Table Layout", root, 760, 560);
        s.showAndWait();
    }

    public void onReportBtn(){
        Button revenue = reportButton("Menu Item\nRevenue", "$");
        revenue.setOnAction(e -> showReportText("Revenue Report", revenueReport()));
        Button popularity = reportButton("Menu Item\nPopularity", "▥");
        popularity.setOnAction(e -> showReportText("Menu Popularity Report", menuReport()));
        Button turnaround = reportButton("Average\nTurnaround Time", "◷");
        turnaround.setOnAction(e -> showReportText("Order Timing Report", timingReport()));
        Button employees = reportButton("Personnel\nEfficiency", "◴");
        employees.setOnAction(e -> showReportText("Employee Report", employeeReport()));
        Button hours = reportButton("Working Hours\nReport", "h");
        hours.setOnAction(e -> showReportText("Working Hours Report", workingHoursReport()));
        Button inventory = reportButton("Inventory\nUsage", "i");
        inventory.setOnAction(e -> showReportText("Inventory Usage Report", inventoryUsageReport()));

        Label title = new Label("Reports:");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ef2b2d;");
        VBox buttons = new VBox(12, revenue, popularity, turnaround, employees, hours, inventory);
        VBox root = new VBox(18, title, buttons);
        root.setPadding(new Insets(18));
        Stage s = simpleDialog("Reports", root, 640, 760);
        s.showAndWait();
    }

    public void onInventoryBtn(){
        AppContext.inventory().ensureKnownIngredients();
        var usage = AppContext.inventory().ingredientUsageFromTickets(AppContext.orders().allTickets());
        TableView<String> table = new TableView<>(FXCollections.observableArrayList(AppContext.inventory().allIngredients()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<String, String> ingredientCol = new TableColumn<>("Ingredient / Supply");
        ingredientCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(cd::getValue));
        TableColumn<String, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> unitForIngredient(cd.getValue())));
        TableColumn<String, String> usedCol = new TableColumn<>("Used");
        usedCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> String.valueOf(usage.getOrDefault(cd.getValue(), 0))));
        TableColumn<String, String> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> String.valueOf(AppContext.inventory().stockForIngredient(cd.getValue()))));
        table.getColumns().addAll(ingredientCol, unitCol, usedCol, stockCol);

        Spinner<Integer> quantity = new Spinner<>(0, 9999, 100);
        quantity.setEditable(true);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, ingredient) -> {
            if (ingredient != null) {
                quantity.getValueFactory().setValue(AppContext.inventory().stockForIngredient(ingredient));
            }
        });
        Button save = new Button("Set Stock");
        save.getStyleClass().add("btn-primary");
        save.setOnAction(e -> {
            String ingredient = table.getSelectionModel().getSelectedItem();
            if (ingredient == null) {
                AlertHelper.error("Inventory", "Select an ingredient.");
                return;
            }
            AppContext.inventory().setIngredientStock(ingredient, quantity.getValue());
            table.refresh();
        });
        Button addTen = new Button("+10");
        addTen.setOnAction(e -> {
            String ingredient = table.getSelectionModel().getSelectedItem();
            if (ingredient == null) return;
            AppContext.inventory().adjustIngredientStock(ingredient, 10);
            quantity.getValueFactory().setValue(AppContext.inventory().stockForIngredient(ingredient));
            table.refresh();
        });
        Button minusTen = new Button("-10");
        minusTen.setOnAction(e -> {
            String ingredient = table.getSelectionModel().getSelectedItem();
            if (ingredient == null) return;
            AppContext.inventory().adjustIngredientStock(ingredient, -10);
            quantity.getValueFactory().setValue(AppContext.inventory().stockForIngredient(ingredient));
            table.refresh();
        });
        Label lowStock = new Label();
        Runnable refreshLowStock = () -> {
            List<String> low = AppContext.inventory().allIngredients().stream()
                    .filter(i -> AppContext.inventory().stockForIngredient(i) <= 10)
                    .toList();
            lowStock.setText(low.isEmpty() ? "Low stock: none" : "Low stock: " + String.join(", ", low));
        };
        refreshLowStock.run();
        save.setOnAction(e -> {
            String ingredient = table.getSelectionModel().getSelectedItem();
            if (ingredient == null) {
                AlertHelper.error("Inventory", "Select an ingredient.");
                return;
            }
            AppContext.inventory().setIngredientStock(ingredient, quantity.getValue());
            table.refresh();
            refreshLowStock.run();
        });

        VBox root = new VBox(10, table, new HBox(10, labeled("Quantity", quantity), save, addTen, minusTen), lowStock);
        root.setPadding(new Insets(12));
        VBox.setVgrow(table, Priority.ALWAYS);
        Stage s = simpleDialog("Ingredient Inventory", root, 760, 560);
        s.showAndWait();
    }

    public void onEmployeeBtn(){
        TableView<Employee> table = new TableView<>(FXCollections.observableArrayList(AppContext.auth().allEmployees()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Employee, String> idCol = new TableColumn<>("Employee ID");
        idCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().employeeId()));
        TableColumn<Employee, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().displayName()));
        TableColumn<Employee, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().username()));
        TableColumn<Employee, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().role().name()));
        table.getColumns().addAll(idCol, nameCol, userCol, roleCol);

        TextField employeeId = new TextField();
        employeeId.setPromptText("ABC123");
        TextField displayName = new TextField();
        displayName.setPromptText("Display name");
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.WAITER, Role.BUSBOY, Role.COOK, Role.MANAGER));
        role.getSelectionModel().select(Role.WAITER);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected == null) return;
            employeeId.setText(selected.employeeId());
            displayName.setText(selected.displayName());
            username.setText(selected.username());
            password.setText(selected.password());
            role.getSelectionModel().select(selected.role());
        });

        Button add = new Button("Add Employee");
        add.getStyleClass().add("btn-primary");
        add.setOnAction(e -> {
            try {
                AppContext.auth().signUp(employeeId.getText(), username.getText(), password.getText(), role.getValue(), displayName.getText());
                AppContext.activity().record(AppContext.session().currentUser(), "Created employee profile " + username.getText());
                table.setItems(FXCollections.observableArrayList(AppContext.auth().allEmployees()));
                employeeId.clear();
                displayName.clear();
                username.clear();
                password.clear();
            } catch (RuntimeException ex) {
                AlertHelper.error("Employees", ex.getMessage());
            }
        });

        Button update = new Button("Update Selected");
        update.setOnAction(e -> {
            Employee selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertHelper.error("Employees", "Select an employee.");
                return;
            }
            try {
                Employee updated = AppContext.auth().updateEmployee(
                        selected,
                        employeeId.getText(),
                        username.getText(),
                        password.getText(),
                        role.getValue(),
                        displayName.getText()
                );
                AppContext.activity().record(AppContext.session().currentUser(), "Modified employee profile " + updated.username());
                table.setItems(FXCollections.observableArrayList(AppContext.auth().allEmployees()));
                table.getSelectionModel().select(updated);
            } catch (RuntimeException ex) {
                AlertHelper.error("Employees", ex.getMessage());
            }
        });

        Button remove = new Button("Remove Selected");
        remove.getStyleClass().add("btn-danger");
        remove.setOnAction(e -> {
            Employee selected = table.getSelectionModel().getSelectedItem();
            Employee me = AppContext.session().currentUser();
            if (selected == null) {
                AlertHelper.error("Employees", "Select an employee.");
                return;
            }
            if (me != null && selected.employeeId().equalsIgnoreCase(me.employeeId())) {
                AlertHelper.error("Employees", "You cannot remove the active manager account.");
                return;
            }
            if (!AlertHelper.confirm("Remove employee", "Remove " + selected.displayName() + "?")) {
                return;
            }
            AppContext.auth().deleteEmployee(selected);
            AppContext.activity().record(AppContext.session().currentUser(), "Removed employee profile " + selected.username());
            table.setItems(FXCollections.observableArrayList(AppContext.auth().allEmployees()));
        });

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("Employee ID"), employeeId, new Label("Name"), displayName);
        form.addRow(1, new Label("Username"), username, new Label("Password"), password);
        form.addRow(2, new Label("Role"), role, add, update);
        form.add(remove, 3, 3);

        VBox root = new VBox(12, table, form);
        root.setPadding(new Insets(12));
        VBox.setVgrow(table, Priority.ALWAYS);
        Stage s = simpleDialog("Employees", root, 760, 560);
        s.showAndWait();
    }

    public void onMenuChangeBtn(){
        TableView<MenuItem> table = new TableView<>(AppContext.menu().allItems());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<MenuItem, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().id()));
        TableColumn<MenuItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().name()));
        TableColumn<MenuItem, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> cd.getValue().category().name()));
        TableColumn<MenuItem, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() -> "$" + money(cd.getValue().price())));
        table.getColumns().addAll(idCol, nameCol, catCol, priceCol);

        TextField id = new TextField();
        TextField name = new TextField();
        TextField price = new TextField();
        TextArea description = new TextArea();
        description.setPrefRowCount(2);
        ComboBox<MenuCategory> category = new ComboBox<>(FXCollections.observableArrayList(MenuCategory.values()));
        category.getSelectionModel().selectFirst();

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, item) -> {
            if (item == null) return;
            id.setText(item.id());
            name.setText(item.name());
            price.setText(money(item.price()));
            description.setText(item.description());
            category.getSelectionModel().select(item.category());
        });

        Button add = new Button("Add");
        add.getStyleClass().add("btn-primary");
        add.setOnAction(e -> {
            try {
                MenuItem created = menuItemFromFields(id, name, category, price, description);
                AppContext.menu().addItem(created);
                AppContext.inventory().ensureKnownIngredients();
                table.getSelectionModel().select(created);
            } catch (RuntimeException ex) {
                AlertHelper.error("Menu Change", ex.getMessage());
            }
        });

        Button update = new Button("Update Selected");
        update.setOnAction(e -> {
            MenuItem selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertHelper.error("Menu Change", "Select a menu item.");
                return;
            }
            try {
                MenuItem replacement = menuItemFromFields(id, name, category, price, description);
                AppContext.menu().updateItem(selected.id(), replacement);
                if (!selected.id().equalsIgnoreCase(replacement.id())) {
                    AppContext.inventory().removeItem(selected.id());
                    AppContext.inventory().ensureKnownIngredients();
                }
                table.getSelectionModel().select(replacement);
            } catch (RuntimeException ex) {
                AlertHelper.error("Menu Change", ex.getMessage());
            }
        });

        Button remove = new Button("Remove Selected");
        remove.getStyleClass().add("btn-danger");
        remove.setOnAction(e -> {
            MenuItem selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertHelper.error("Menu Change", "Select a menu item.");
                return;
            }
            if (!AlertHelper.confirm("Remove menu item", "Remove " + selected.name() + "?")) {
                return;
            }
            AppContext.menu().removeItem(selected);
            AppContext.inventory().removeItem(selected.id());
            id.clear();
            name.clear();
            price.clear();
            description.clear();
        });

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("ID"), id, new Label("Name"), name);
        form.addRow(1, new Label("Category"), category, new Label("Price"), price);
        form.add(new Label("Description"), 0, 2);
        form.add(description, 1, 2, 3, 1);
        form.add(new HBox(10, add, update, remove), 1, 3, 3, 1);

        VBox root = new VBox(12, table, form);
        root.setPadding(new Insets(12));
        VBox.setVgrow(table, Priority.ALWAYS);
        Stage s = simpleDialog("Menu Change", root, 820, 620);
        s.showAndWait();
    }

    public void onMarkOpen() {
        if (selectedTable == null) {
            return;
        }
        try {
            AppContext.tables().attemptStatusChange(Role.MANAGER, selectedTable, TableStatus.OPEN);
        } catch (RuntimeException ex) {
            AlertHelper.error("Status change", ex.getMessage());
        }
        refreshSelectedPanel();
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
        RestaurantTable primary = AppContext.tables().primaryTableFor(selectedTable);
        primary.setGuestCount(guests);
        try {
            AppContext.tables().attemptStatusChange(Role.MANAGER, selectedTable, TableStatus.OCCUPIED);
        } catch (RuntimeException ex) {
            AlertHelper.error("Status change", ex.getMessage());
        }
        refreshSelectedPanel();
    }

    public void onMarkDirty() {
        if (selectedTable == null) {
            AlertHelper.error("Select a table", "Error: Please select a table first");
            return;
        }
        try {
            requireClockedIn();
            // Managers cannot mark an occupied table dirty while an unpaid check is still open.
            Ticket t = AppContext.orders().latestTicketForTable(selectedTable.id()).orElse(null);
            boolean blocking = t != null
                    && t.status() != OrderStatus.PAID
                    && !(t.status() == OrderStatus.DRAFT && t.items().isEmpty());
            if (blocking) {
                AlertHelper.error("Checkout required", "Error: Please complete checkout/payment before marking table dirty.");
                return;
            }
            AppContext.tables().attemptStatusChange(Role.MANAGER, selectedTable, TableStatus.DIRTY);
        } catch (IllegalArgumentException ex) {
            AlertHelper.error("Status change", ex.getMessage());
        } catch (IllegalStateException ex) {
            AlertHelper.error("Protected action", ex.getMessage());
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
            AlertHelper.info("Closed check history", "There are no closed checks in history yet.");
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

    public void onOpenTicket() {
        if (selectedTable == null) {
            AlertHelper.error("Select a table", "Error: Please select a table first");
            return;
        }
        try {
            requireClockedIn();
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

        // The floor plan uses labels around a fixed A-F by 1-6 grid.
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

        // Place the 28 default tables around the kitchen block.
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

        // Center kitchen block.
        VBox kitchen = new VBox();
        kitchen.setMinSize(220, 320);
        kitchen.setStyle("-fx-border-color: #2563eb; -fx-border-width: 2; -fx-background-color: transparent;");
        floorGrid.add(kitchen, 3, 1, 2, 4);

        int extraCol = 1;
        int extraRow = 9;
        for (RestaurantTable table : AppContext.tables().allTables()) {
            if (tableButtons.containsKey(table.id())) {
                continue;
            }
            Button b = createTableButton(table);
            floorGrid.add(b, extraCol, extraRow);
            tableButtons.put(table.id(), b);
            extraCol++;
            if (extraCol > 6) {
                extraCol = 1;
                extraRow++;
            }
        }
    }

    private void addTableButton(char col, int row) {
        String id = "" + col + row;
        RestaurantTable table = AppContext.tables().requireTable(id);
        Button b = createTableButton(table);
        int gridCol = (col - 'A') + 1;
        int gridRow = row;
        floorGrid.add(b, gridCol, gridRow);
        tableButtons.put(id, b);
    }

    private Button createTableButton(RestaurantTable table) {
        Button b = new Button(table.id());
        b.getStyleClass().addAll("table-btn");
        applyStatusStyle(b, table.status());
        table.statusProperty().addListener((obs, oldV, newV) -> applyStatusStyle(b, newV));
        b.setOnAction(e -> onTableClicked(table));
        return b;
    }

    private void onTableClicked(RestaurantTable table) {
        setSelected(table);
    }

    private void setSelected(RestaurantTable table) {
        this.selectedTable = table;
        refreshSelectedPanel();
    }

    private void refreshClockUi() {
        clockButton.setText("Manager");
        clockButton.setDisable(true);
        elapsedLabel.setText("");
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
            markOccupiedBtn.setDisable(false);
            markDirtyBtn.setDisable(false);
            openTicketBtn.setDisable(false);
            checkoutBtn.setDisable(false);
            return;
        }

        RestaurantTable primary = AppContext.tables().primaryTableFor(selectedTable);
        selectedTableLabel.setText(AppContext.tables().displayIdForGroup(selectedTable));
        selectedStatusLabel.setText("Status: " + displayStatus(primary.status()));
        String joined = AppContext.tables().joinedGroupFor(selectedTable).size() > 1 ? " · Joined table group" : "";
        assignedLabel.setText("Waiter: " + waiterName(primary.assignedWaiterId())
                + " · Seats: " + AppContext.tables().totalSeatsForGroup(selectedTable)
                + " · Guests: " + primary.guestCount()
                + joined);
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
        // Managers are allowed to perform table and order actions without clocking in.
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
        int maxSeats = selectedTable == null ? 4 : Math.max(1, AppContext.tables().totalSeatsForGroup(selectedTable));
        Spinner<Integer> sp = new Spinner<>(1, maxSeats, Math.min(2, maxSeats));
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

        int seatMax = Math.max(1, AppContext.tables().totalSeatsForGroup(AppContext.tables().requireTable(ticket.tableId())));
        Spinner<Integer> seatSpinner = new Spinner<>(1, seatMax, 1);
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

    private Button reportButton(String label, String icon) {
        Label text = new Label(label);
        text.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Label mark = new Label(icon);
        mark.setStyle("-fx-font-size: 58px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox graphic = new HBox(18, text, spacer, mark);
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.setPrefWidth(520);
        Button button = new Button();
        button.setGraphic(graphic);
        button.setMinSize(560, 112);
        button.setStyle("-fx-background-color: #d1d1d1; -fx-background-radius: 0;");
        return button;
    }

    private void showReportText(String title, String text) {
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(false);
        area.setStyle("-fx-font-family: 'Menlo', 'Monospaced'; -fx-font-size: 13px;");
        Stage s = simpleDialog(title, area, 860, 640);
        s.showAndWait();
    }

    private String revenueReport() {
        BigDecimal foodSubtotal = BigDecimal.ZERO;
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal tips = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        int paid = 0;
        int open = 0;
        Map<String, BigDecimal> revenueByItem = new HashMap<>();

        for (Ticket t : AppContext.orders().allTickets()) {
            if (OrderService.isGhostDraftTicket(t)) continue;
            foodSubtotal = foodSubtotal.add(t.subtotal());
            gross = gross.add(AppContext.orders().ticketTotalWithTax(t));
            collected = collected.add(AppContext.orders().paidTotal(t));
            tips = tips.add(t.recordedTipTotal());
            if (t.status() == OrderStatus.PAID) paid++; else open++;
            for (var payment : t.payments()) {
                if (payment.amount().signum() < 0) refunds = refunds.add(payment.amount().abs());
            }
            for (TicketItem line : t.items()) {
                revenueByItem.merge(line.menuItem().name(), line.lineTotal(), BigDecimal::add);
            }
        }

        StringBuilder out = new StringBuilder("REVENUE REPORT\n\n");
        out.append("Food subtotal:          $").append(money(foodSubtotal)).append('\n');
        out.append("Gross with tax:         $").append(money(gross)).append('\n');
        out.append("Collected payments:     $").append(money(collected)).append('\n');
        out.append("Refunds issued:         $").append(money(refunds)).append('\n');
        out.append("Recorded tips:          $").append(money(tips)).append('\n');
        out.append("Open checks:            ").append(open).append('\n');
        out.append("Paid checks:            ").append(paid).append("\n\n");
        out.append("Menu item revenue\n");
        revenueByItem.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .forEach(e -> out.append(pad(e.getKey(), 34)).append(" $").append(money(e.getValue())).append('\n'));
        if (revenueByItem.isEmpty()) out.append("No revenue yet.\n");
        return out.toString();
    }

    private String menuReport() {
        Map<String, Integer> qtyByItem = new HashMap<>();
        Map<String, BigDecimal> revenueByItem = new HashMap<>();
        Map<MenuCategory, Integer> qtyByCategory = new HashMap<>();
        for (Ticket t : AppContext.orders().allTickets()) {
            if (OrderService.isGhostDraftTicket(t)) continue;
            for (TicketItem line : t.items()) {
                qtyByItem.merge(line.menuItem().name(), line.quantity(), Integer::sum);
                revenueByItem.merge(line.menuItem().name(), line.lineTotal(), BigDecimal::add);
                qtyByCategory.merge(line.menuItem().category(), line.quantity(), Integer::sum);
            }
        }

        StringBuilder out = new StringBuilder("MENU REPORT\n\n");
        out.append("Popularity by menu item\n");
        qtyByItem.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .forEach(e -> out.append(pad(e.getKey(), 34))
                        .append(" qty ").append(pad(String.valueOf(e.getValue()), 5))
                        .append(" revenue $").append(money(revenueByItem.getOrDefault(e.getKey(), BigDecimal.ZERO))).append('\n'));
        if (qtyByItem.isEmpty()) out.append("No ordered menu items yet.\n");

        out.append("\nPopularity by category\n");
        qtyByCategory.entrySet().stream()
                .sorted(Map.Entry.<MenuCategory, Integer>comparingByValue(Comparator.reverseOrder()))
                .forEach(e -> out.append(pad(e.getKey().name(), 18)).append(e.getValue()).append('\n'));
        return out.toString();
    }

    private String timingReport() {
        long closedCount = 0;
        java.time.Duration totalClosed = java.time.Duration.ZERO;
        java.time.Duration openAgeTotal = java.time.Duration.ZERO;
        int openCount = 0;
        StringBuilder detail = new StringBuilder();
        for (Ticket t : AppContext.orders().allTickets()) {
            if (OrderService.isGhostDraftTicket(t)) continue;
            Instant end = t.payments().stream().map(p -> p.at()).max(Comparator.naturalOrder()).orElse(null);
            if (end != null && t.status() == OrderStatus.PAID) {
                java.time.Duration d = java.time.Duration.between(t.createdAt(), end).abs();
                totalClosed = totalClosed.plus(d);
                closedCount++;
                detail.append("Order #").append(t.orderNumber()).append(" table ").append(t.tableId())
                        .append(" closed in ").append(durationText(d)).append('\n');
            } else {
                java.time.Duration age = java.time.Duration.between(t.createdAt(), Instant.now()).abs();
                openAgeTotal = openAgeTotal.plus(age);
                openCount++;
            }
        }
        StringBuilder out = new StringBuilder("ORDER TIMING REPORT\n\n");
        out.append("Average turnaround time: ").append(closedCount == 0 ? "n/a" : durationText(totalClosed.dividedBy(closedCount))).append('\n');
        out.append("Average open order age:  ").append(openCount == 0 ? "n/a" : durationText(openAgeTotal.dividedBy(openCount))).append('\n');
        out.append("Average order prep time: currently estimated by order turnaround because kitchen ready timestamps are not separately captured.\n\n");
        out.append("Closed order detail\n").append(detail.isEmpty() ? "No closed orders yet.\n" : detail);
        return out.toString();
    }

    private String employeeReport() {
        Map<String, Integer> ticketsByEmployee = new HashMap<>();
        Map<String, Integer> itemsByEmployee = new HashMap<>();
        Map<String, BigDecimal> salesByEmployee = new HashMap<>();
        for (Ticket t : AppContext.orders().allTickets()) {
            if (OrderService.isGhostDraftTicket(t)) continue;
            ticketsByEmployee.merge(t.waiterId(), 1, Integer::sum);
            itemsByEmployee.merge(t.waiterId(), t.items().stream().mapToInt(TicketItem::quantity).sum(), Integer::sum);
            salesByEmployee.merge(t.waiterId(), AppContext.orders().ticketTotalWithTax(t), BigDecimal::add);
        }

        StringBuilder out = new StringBuilder("EMPLOYEE AND PERSONNEL EFFICIENCY REPORT\n\n");
        for (Employee e : AppContext.auth().allEmployees()) {
            out.append(e.employeeId()).append(" | ").append(pad(e.displayName(), 18)).append(" | ").append(e.role()).append('\n');
            if (e.role() == Role.WAITER) {
                long assigned = AppContext.tables().allTables().stream().filter(t -> t.isAssignedTo(e.employeeId())).count();
                out.append("  assigned tables: ").append(assigned)
                        .append(" | tickets: ").append(ticketsByEmployee.getOrDefault(e.employeeId(), 0))
                        .append(" | items: ").append(itemsByEmployee.getOrDefault(e.employeeId(), 0))
                        .append(" | sales: $").append(money(salesByEmployee.getOrDefault(e.employeeId(), BigDecimal.ZERO))).append('\n');
            }
            long activityCount = AppContext.activity().all().stream().filter(line -> line.contains(" | " + e.employeeId() + " | ")).count();
            out.append("  activity entries: ").append(activityCount).append("\n\n");
        }
        return out.toString();
    }

    private String workingHoursReport() {
        Map<String, Instant> activeClockIns = new HashMap<>();
        Map<String, java.time.Duration> totals = new HashMap<>();
        for (String line : AppContext.activity().all()) {
            String[] parts = line.split("\\|", 5);
            if (parts.length < 5) continue;
            Instant at;
            try {
                at = Instant.parse(parts[0].trim());
            } catch (RuntimeException ex) {
                continue;
            }
            String employeeId = parts[1].trim();
            String action = parts[4].trim();
            if (action.startsWith("Clocked in")) {
                activeClockIns.put(employeeId, at);
            } else if (action.startsWith("Clocked out")) {
                Instant start = activeClockIns.remove(employeeId);
                if (start != null && !at.isBefore(start)) {
                    totals.merge(employeeId, java.time.Duration.between(start, at), java.time.Duration::plus);
                }
            }
        }
        Instant now = Instant.now();
        activeClockIns.forEach((id, start) -> totals.merge(id, java.time.Duration.between(start, now), java.time.Duration::plus));

        StringBuilder out = new StringBuilder("WORKING HOURS REPORT\n\n");
        for (Employee e : AppContext.auth().allEmployees()) {
            out.append(e.employeeId()).append(" | ").append(pad(e.displayName(), 18))
                    .append(" | ").append(pad(e.role().name(), 8))
                    .append(" | ").append(durationText(totals.getOrDefault(e.employeeId(), java.time.Duration.ZERO)));
            if (activeClockIns.containsKey(e.employeeId())) out.append(" (clocked in)");
            out.append('\n');
        }
        return out.toString();
    }

    private String inventoryUsageReport() {
        Map<String, Integer> usage = AppContext.inventory().ingredientUsageFromTickets(AppContext.orders().allTickets());
        StringBuilder out = new StringBuilder("INVENTORY USAGE REPORT\n\n");
        out.append(pad("Ingredient", 24)).append(pad("Used", 10)).append(pad("Stock", 10)).append("Unit\n");
        for (String ingredient : AppContext.inventory().allIngredients()) {
            out.append(pad(ingredient, 24))
                    .append(pad(String.valueOf(usage.getOrDefault(ingredient, 0)), 10))
                    .append(pad(String.valueOf(AppContext.inventory().stockForIngredient(ingredient)), 10))
                    .append(unitForIngredient(ingredient)).append('\n');
        }
        return out.toString();
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String pad(String value, int width) {
        String v = value == null ? "" : value;
        return v.length() >= width ? v.substring(0, width) : v + " ".repeat(width - v.length());
    }

    private static String durationText(java.time.Duration duration) {
        if (duration == null) return "00:00";
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%02dh %02dm", hours, minutes);
    }

    private static String unitForIngredient(String ingredient) {
        if (ingredient == null) return "units";
        String s = ingredient.toLowerCase();
        if (s.contains("water")) return "bottles";
        if (s.contains("patties")) return "each";
        if (s.contains("syrup") || s.contains("tea")) return "servings";
        return "portions";
    }

    private String waiterName(String waiterId) {
        if (waiterId == null || waiterId.isBlank()) {
            return "Unassigned";
        }
        return AppContext.auth().allEmployees().stream()
                .filter(e -> e.employeeId().equalsIgnoreCase(waiterId))
                .findFirst()
                .map(e -> e.displayName() + " (" + e.employeeId() + ")")
                .orElse(waiterId);
    }

    private static ListCell<Employee> employeeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName() + " (" + item.employeeId() + ")");
            }
        };
    }

    private static MenuItem menuItemFromFields(TextField id, TextField name, ComboBox<MenuCategory> category,
                                               TextField price, TextArea description) {
        String itemId = id.getText() == null ? "" : id.getText().trim();
        String itemName = name.getText() == null ? "" : name.getText().trim();
        String priceText = price.getText() == null ? "" : price.getText().trim();
        if (itemId.isBlank()) {
            throw new IllegalArgumentException("Menu item ID is required.");
        }
        if (itemName.isBlank()) {
            throw new IllegalArgumentException("Menu item name is required.");
        }
        BigDecimal parsedPrice;
        try {
            parsedPrice = new BigDecimal(priceText).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid price.");
        }
        if (parsedPrice.signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        MenuCategory selectedCategory = category.getValue();
        if (selectedCategory == null) {
            throw new IllegalArgumentException("Select a category.");
        }
        return new MenuItem(itemId, itemName, selectedCategory, parsedPrice, description.getText());
    }

    private static VBox labeled(String label, javafx.scene.Node node) {
        VBox v = new VBox(4, new Label(label), node);
        return v;
    }
}
