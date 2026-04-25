package com.jjcorner.app.ui.checkout;

import com.jjcorner.app.AppContext;
import com.jjcorner.app.model.OrderStatus;
import com.jjcorner.app.model.Payment;
import com.jjcorner.app.model.PaymentMethod;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.model.Ticket;
import com.jjcorner.app.model.TicketItem;
import com.jjcorner.app.service.OrderService;
import com.jjcorner.app.util.AlertHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * View a paid (closed) check: full line detail, reprint / email receipt stubs, and line refunds.
 */
public final class ClosedCheckDialog {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final Ticket ticket;
    private final OrderService orders = AppContext.orders();
    private final ObservableList<RefundRow> rows = FXCollections.observableArrayList();
    private final ToggleGroup payTypeGroup = new ToggleGroup();
    private final ToggleButton cashToggle = new ToggleButton("Cash refund");
    private final ToggleButton cardToggle = new ToggleButton("Card refund");
    private final Label metaLabel = new Label();
    private final Label totalsLabel = new Label();
    private TableView<RefundRow> table;

    private ClosedCheckDialog(Ticket ticket) {
        this.ticket = Objects.requireNonNull(ticket);
    }

    public static void show(Window owner, Ticket ticket) {
        if (ticket == null) {
            return;
        }
        if (!AppContext.orders().mayOpenClosedCheckDialog(ticket)) {
            AlertHelper.error("Closed check", "This check is not in closed history or cannot be opened here.");
            return;
        }
        ClosedCheckDialog d = new ClosedCheckDialog(ticket);
        Stage st = d.buildStage(owner);
        st.showAndWait();
    }

    private Stage buildStage(Window owner) {
        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Check history · Table " + ticket.tableId() + " · Order #" + ticket.orderNumber());

        cashToggle.setToggleGroup(payTypeGroup);
        cardToggle.setToggleGroup(payTypeGroup);
        cashToggle.setUserData(PaymentMethod.CASH);
        cardToggle.setUserData(PaymentMethod.CARD);
        cashToggle.setSelected(true);
        payTypeGroup.selectedToggleProperty().addListener((o, p, n) -> {
            if (n == null) {
                cashToggle.setSelected(true);
            }
        });

        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Button printBtn = new Button("Print receipt");
        printBtn.setOnAction(e -> onPrintReceipt(stage));
        Button emailBtn = new Button("Email receipt");
        emailBtn.setOnAction(e -> onEmailReceipt(stage));
        Button refundBtn = new Button("Issue refund for selected lines");
        refundBtn.getStyleClass().add("btn-primary");
        refundBtn.setOnAction(e -> onIssueRefund(stage));
        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> stage.close());

        HBox typeRow = new HBox(10, cashToggle, cardToggle);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(10, printBtn, emailBtn, refundBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(12, metaLabel, totalsLabel, table, new Label("Payment method for refund:"), typeRow, actions);
        root.setPadding(new Insets(16));
        VBox.setVgrow(table, Priority.ALWAYS);

        BorderPane bp = new BorderPane();
        bp.setCenter(root);

        Scene scene = new Scene(bp, 920, 560);
        var url = ClosedCheckDialog.class.getResource("/com/jjcorner/view/styles.css");
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
        stage.setScene(scene);

        orders.syncLinePaidQuantitiesForPaidTicket(ticket);

        Runnable refreshMeta = this::refreshMetaAndTotals;
        Runnable rebuildRows = () -> rows.setAll(ticket.items().stream().map(RefundRow::new).toList());

        Consumer<TicketItem> wireRowTotals = it -> {
            it.paidQuantityProperty().addListener((x, a, b) -> refreshMeta.run());
            it.quantityProperty().addListener((x, a, b) -> refreshMeta.run());
            it.refundedQuantityProperty().addListener((x, a, b) -> refreshMeta.run());
        };
        ticket.items().forEach(wireRowTotals);

        ticket.items().addListener((ListChangeListener<TicketItem>) ch -> {
            while (ch.next()) {
                for (TicketItem it : ch.getAddedSubList()) {
                    wireRowTotals.accept(it);
                }
            }
            rebuildRows.run();
            refreshMeta.run();
        });
        ticket.payments().addListener((ListChangeListener<Payment>) c -> refreshMeta.run());
        ticket.statusProperty().addListener((o, a, b) -> {
            rebuildRows.run();
            refreshMeta.run();
        });

        rebuildRows.run();
        refreshMeta.run();

        return stage;
    }

    private TableView<RefundRow> buildTable() {
        TableView<RefundRow> tv = new TableView<>(rows);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<RefundRow, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(cd -> {
            TicketItem i = cd.getValue().item();
            return javafx.beans.binding.Bindings.createStringBinding(() -> {
                String n = i.menuItem().name();
                int rq = i.refundedQuantity();
                if (rq <= 0) {
                    return n;
                }
                return n + " — Refunded" + (rq > 1 ? " (" + rq + ")" : "");
            }, i.refundedQuantityProperty());
        });

        TableColumn<RefundRow, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setMaxWidth(70);
        qtyCol.setCellValueFactory(cd -> cd.getValue().item().quantityProperty().asObject());

        TableColumn<RefundRow, String> paidCol = new TableColumn<>("Paid");
        paidCol.setMaxWidth(90);
        paidCol.setCellValueFactory(cd -> {
            TicketItem i = cd.getValue().item();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> i.paidQuantity() + " / " + i.quantity(),
                    i.paidQuantityProperty(),
                    i.quantityProperty());
        });

        TableColumn<RefundRow, String> lineCol = new TableColumn<>("Line total");
        lineCol.setCellValueFactory(cd -> {
            TicketItem i = cd.getValue().item();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> formatMoney(i.lineTotal()),
                    i.quantityProperty(),
                    i.unitExtraProperty());
        });

        TableColumn<RefundRow, Boolean> refundCol = new TableColumn<>("Refund");
        refundCol.setMaxWidth(80);
        refundCol.setCellValueFactory(cd -> cd.getValue().refundProperty());
        refundCol.setCellFactory(col -> new TableCell<RefundRow, Boolean>() {
            private final CheckBox cb = new CheckBox();

            {
                cb.setOnAction(ev -> {
                    TableRow<RefundRow> row = getTableRow();
                    if (row == null || row.getItem() == null || cb.isDisabled()) {
                        return;
                    }
                    RefundRow rr = row.getItem();
                    if (rr.item().refundedQuantity() > 0 || rr.item().paidQuantity() <= 0 || !tv.isEditable()) {
                        rr.refund.set(false);
                        cb.setSelected(false);
                        return;
                    }
                    rr.refund.set(!rr.refund.get());
                    cb.setSelected(rr.refund.get());
                });
            }

            @Override
            protected void updateItem(Boolean ignored, boolean empty) {
                super.updateItem(ignored, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                RefundRow rr = getTableRow().getItem();
                TicketItem it = rr.item();
                boolean can = it.refundedQuantity() <= 0 && it.paidQuantity() > 0 && tv.isEditable();
                cb.setDisable(!can);
                if (!can) {
                    rr.refund.set(false);
                }
                cb.setSelected(Boolean.TRUE.equals(rr.refund.get()) && can);
                setGraphic(cb);
            }
        });

        tv.getColumns().addAll(List.of(nameCol, qtyCol, paidCol, lineCol, refundCol));
        return tv;
    }

    private void refreshMetaAndTotals() {
        Instant ts = ticket.createdAt();
        String timeStr = ts == null ? "" : TIME_FMT.format(ts.atZone(DISPLAY_ZONE));
        String refundTag = OrderService.ticketHasRefunds(ticket) ? "   ·   Refunds on file" : "";
        metaLabel.setText("Order #" + ticket.orderNumber()
                + "   ·   " + ticket.guestCount() + " guests"
                + "   ·   Opened " + timeStr
                + "   ·   Status: " + ticket.status()
                + refundTag);

        BigDecimal sub = ticket.subtotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = orders.tax(ticket.subtotal());
        BigDecimal foodTax = orders.ticketTotalWithTax(ticket);
        BigDecimal due = orders.ticketAmountDue(ticket);
        BigDecimal paid = orders.paidTotal(ticket);
        BigDecimal rem = orders.remainingTotal(ticket);
        totalsLabel.setText("Subtotal " + formatMoney(sub) + "   Tax " + formatMoney(tax)
                + "   Food & tax " + formatMoney(foodTax) + "   Total due " + formatMoney(due) + "   Paid " + formatMoney(paid)
                + "   Balance " + formatMoney(rem));

        boolean refundEditable = ticket.status() == OrderStatus.PAID
                || (ticket.status() == OrderStatus.PENDING
                && orders.paidTotal(ticket).compareTo(BigDecimal.ZERO) > 0);
        table.setEditable(refundEditable);
        for (TableColumn<RefundRow, ?> c : table.getColumns()) {
            c.setEditable(refundEditable);
        }
    }

    private void onIssueRefund(Stage stage) {
        table.edit(-1, null);
        if (!orders.mayOpenClosedCheckDialog(ticket)) {
            AlertHelper.error("Refund", "No further refunds can be issued from here. Use Checkout for any open balance.");
            return;
        }
        List<TicketItem> selected = rows.stream()
                .filter(r -> Boolean.TRUE.equals(r.refund.get()))
                .map(RefundRow::item)
                .filter(it -> it.paidQuantity() > 0 && it.refundedQuantity() <= 0)
                .toList();
        if (selected.isEmpty()) {
            AlertHelper.error("Refund", "Select one or more paid lines that have not been refunded yet.");
            return;
        }
        if (!managerApproval(stage)) {
            return;
        }
        PaymentMethod method = cashToggle.isSelected() ? PaymentMethod.CASH : PaymentMethod.CARD;
        try {
            orders.recordRefundForPaidLines(ticket, selected, method);
        } catch (RuntimeException ex) {
            AlertHelper.error("Refund", ex.getMessage());
            return;
        }
        AlertHelper.info("Refund", "Refund issued.");
        stage.close();
    }

    private boolean managerApproval(Window owner) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Manager approval required");

        Label title = new Label("Manager approval required");
        title.getStyleClass().add("checkout-section-title");

        TextField userField = new TextField();
        PasswordField passField = new PasswordField();
        userField.setPromptText("Username");
        passField.setPromptText("Password");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Username"), 0, 0);
        grid.add(userField, 1, 0);
        grid.add(new Label("Password"), 0, 1);
        grid.add(passField, 1, 1);

        Label err = new Label(" ");
        err.setStyle("-fx-text-fill: #dc2626;");

        final boolean[] approved = {false};

        Button approve = new Button("Approve");
        approve.getStyleClass().add("btn-primary");
        Button cancel = new Button("Cancel");
        approve.setOnAction(e -> {
            var u = userField.getText();
            var p = passField.getText();
            var ok = AppContext.auth().login(u, p)
                    .filter(emp -> emp.role() == Role.MANAGER)
                    .isPresent();
            if (!ok) {
                err.setText("Invalid manager credentials.");
                return;
            }
            approved[0] = true;
            dlg.close();
        });
        cancel.setOnAction(e -> dlg.close());

        HBox actions = new HBox(10, cancel, approve);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, title, grid, err, actions);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 420, 220);
        var url = ClosedCheckDialog.class.getResource("/com/jjcorner/view/styles.css");
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
        dlg.setScene(scene);
        dlg.showAndWait();
        if (!approved[0]) {
            AlertHelper.info("Refund", "Manager approval required.");
        }
        return approved[0];
    }

    private void onPrintReceipt(Stage owner) {
        AlertHelper.info("Receipt", "Order #" + ticket.orderNumber() + " · Table " + ticket.tableId()
                + " · " + ticket.guestCount() + " guests\nReceipt printed.");
    }

    private void onEmailReceipt(Stage owner) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.initOwner(owner);
        dlg.setTitle("Email receipt");
        dlg.setHeaderText("Enter the guest email address");
        dlg.setContentText("Email:");
        dlg.getEditor().setPromptText("name@example.com");
        var ok = dlg.getDialogPane().lookupButton(ButtonType.OK);
        if (ok instanceof Button submitBtn) {
            submitBtn.setText("Submit");
        }
        dlg.showAndWait().ifPresent(raw -> {
            String email = raw == null ? "" : raw.trim();
            if (email.isBlank()) {
                AlertHelper.error("Email receipt", "Enter an email address.");
                return;
            }
            if (!email.contains("@") || email.length() < 5) {
                AlertHelper.error("Email receipt", "Enter a valid email address.");
                return;
            }
            AlertHelper.info("Receipt sent", "Order #" + ticket.orderNumber() + " · Table " + ticket.tableId()
                    + " · " + ticket.guestCount() + " guests\nReceipt sent to " + email + ".");
        });
    }

    private static String formatMoney(BigDecimal v) {
        return "$" + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static final class RefundRow {
        private final TicketItem item;
        private final BooleanProperty refund = new SimpleBooleanProperty(false);

        RefundRow(TicketItem item) {
            this.item = Objects.requireNonNull(item);
        }

        TicketItem item() {
            return item;
        }

        BooleanProperty refundProperty() {
            return refund;
        }
    }
}
