package com.jjcorner.app.ui.checkout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.jjcorner.app.AppContext;
import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.OrderStatus;
import com.jjcorner.app.model.Payment;
import com.jjcorner.app.model.PaymentMethod;
import com.jjcorner.app.model.Ticket;
import com.jjcorner.app.model.TicketItem;
import com.jjcorner.app.service.OrderService;
import com.jjcorner.app.util.AlertHelper;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Minimal restaurant POS checkout: large touch targets, clear hierarchy, fast paths.
 */
public final class CheckoutScreen {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final Ticket ticket;
    private final Stage stage;
    private final OrderService orders = AppContext.orders();

    private final Label msgLabel = new Label(" ");
    private final TextField payAmountField = new TextField();
    private final TextField tenderedField = new TextField();
    private final Label changeLabel = new Label("$0.00");
    private final VBox cashBox = new VBox(10);
    private final ToggleGroup payTypeGroup = new ToggleGroup();
    private final ToggleButton cashToggle = new ToggleButton("Cash");
    private final ToggleButton cardToggle = new ToggleButton("Card");
    private final Label statusBadge = new Label();
    private final Label subtotalVal = new Label();
    private final Label taxVal = new Label();
    private final Label discountVal = new Label("$0.00");
    private final Label totalFoodVal = new Label();
    private final Label paidVal = new Label();
    private final Label balanceVal = new Label();
    private final Button finalizeBtn = new Button("Finalize ticket");
    private final FlowPane seatSplitBox = new FlowPane(8, 8);
    private boolean evenSplitMode = false;

    public CheckoutScreen(Ticket ticket, Stage stage) {
        this.ticket = Objects.requireNonNull(ticket);
        this.stage = Objects.requireNonNull(stage);
    }

    public static void show(Ticket ticket) {
        if (ticket.items().isEmpty()) {
            AlertHelper.error("Checkout", "Cannot checkout an empty ticket.");
            return;
        }
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Checkout · Table " + ticket.tableId() + " · Order #" + ticket.orderNumber());
        CheckoutScreen screen = new CheckoutScreen(ticket, s);
        BorderPane root = screen.buildRoot();
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double[] wh = fitCheckoutWindow(vb);
        Scene scene = new Scene(root, wh[0], wh[1]);
        var url = CheckoutScreen.class.getResource("/com/jjcorner/view/styles.css");
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
        root.getStyleClass().add("checkout-screen");
        s.setScene(scene);
        s.setX(vb.getMinX() + (vb.getWidth() - wh[0]) / 2);
        s.setY(vb.getMinY() + (vb.getHeight() - wh[1]) / 2);
        s.setMaxWidth(vb.getWidth());
        s.setMaxHeight(vb.getHeight());
        s.showAndWait();
    }

    /** Width and height that fit on the primary display with margins (never larger than ~920×600). */
    private static double[] fitCheckoutWindow(Rectangle2D vb) {
        double margin = 24;
        double maxW = vb.getWidth() - 2 * margin;
        double maxH = vb.getHeight() - 2 * margin;
        double w = Math.clamp(maxW, 640, 920);
        double h = Math.clamp(maxH, 420, 600);
        return new double[]{w, h};
    }

    private BorderPane buildRoot() {
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        ScrollPane scroll = new ScrollPane(buildBody());
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: #f3f4f6;");
        scroll.getStyleClass().add("checkout-scroll");
        root.setCenter(scroll);
        wireRefresh();
        refreshAll();
        return root;
    }

    private void wireRefresh() {
        Runnable r = this::refreshAll;
        ticket.payments().addListener((ListChangeListener<Payment>) c -> r.run());
        ticket.statusProperty().addListener((o, a, b) -> r.run());
        ticket.items().addListener((ListChangeListener<TicketItem>) ch -> {
            while (ch.next()) {
                for (TicketItem it : ch.getAddedSubList()) {
                    it.paidQuantityProperty().addListener((o, a, b) -> r.run());
                    it.quantityProperty().addListener((o, a, b) -> r.run());
                    it.refundedQuantityProperty().addListener((o, a, b) -> r.run());
                }
            }
            r.run();
        });
        ticket.items().forEach(it -> {
            it.paidQuantityProperty().addListener((o, a, b) -> r.run());
            it.quantityProperty().addListener((o, a, b) -> r.run());
            it.refundedQuantityProperty().addListener((o, a, b) -> r.run());
        });
        payAmountField.textProperty().addListener((o, a, b) -> refreshChangeOnly());
        tenderedField.textProperty().addListener((o, a, b) -> refreshChangeOnly());
        cashToggle.selectedProperty().addListener((o, a, cashOn) -> {
            cashBox.setVisible(Boolean.TRUE.equals(cashOn));
            cashBox.setManaged(Boolean.TRUE.equals(cashOn));
            refreshChangeOnly();
        });
        payTypeGroup.selectedToggleProperty().addListener((obs, prev, sel) -> {
            if (sel == null) {
                cashToggle.setSelected(true);
            }
        });
    }

    private HBox buildHeader() {
        Button back = new Button("← Back");
        back.getStyleClass().addAll("touch-btn", "checkout-back");
        back.setOnAction(e -> stage.close());

        ImageView logo = smallLogo();

        String ticketNo = formatTicketNo(ticket.id());
        Instant ts = ticket.createdAt();
        String timeStr = ts == null ? "" : TIME_FMT.format(ts.atZone(DISPLAY_ZONE));

        Label line1 = new Label("Table " + ticket.tableId()
                + "   ·   Order #" + ticket.orderNumber()
                + "   ·   Ticket " + ticketNo
                + "   ·   " + ticket.guestCount() + " guests");
        line1.getStyleClass().add("checkout-header-title");
        Label line2 = new Label("Opened " + timeStr);
        line2.getStyleClass().add("checkout-header-meta");

        VBox titles = new VBox(4, line1, line2);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String server = resolveServerLabel();
        Label serverLbl = new Label("Server: " + server);
        serverLbl.getStyleClass().add("checkout-header-meta");
        statusBadge.getStyleClass().setAll("checkout-status-badge");
        VBox rightMeta = new VBox(6, serverLbl, statusBadge);
        rightMeta.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox(16, back, logo, titles, spacer, rightMeta);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.getStyleClass().add("checkout-header");
        return bar;
    }

    private javafx.scene.Node buildBody() {
        TableView<TicketItem> table = buildItemsTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Label billTitle = new Label("Bill");
        billTitle.getStyleClass().add("checkout-section-title");

        VBox left = new VBox(12, billTitle, table);
        left.setPadding(new Insets(16, 12, 16, 20));
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMinWidth(260);

        VBox right = buildRightPanel();
        right.setPadding(new Insets(16, 20, 16, 12));
        right.setMinWidth(260);
        right.setPrefWidth(300);

        HBox row = new HBox(12, left, right);
        row.setFillHeight(true);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.NEVER);
        return row;
    }

    private TableView<TicketItem> buildItemsTable() {
        TableView<TicketItem> tv = new TableView<>(ticket.items());
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setFixedCellSize(44);
        tv.getStyleClass().add("checkout-table");

        TableColumn<TicketItem, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(cd -> Bindings.createStringBinding(() -> cd.getValue().menuItem().name()));

        TableColumn<TicketItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setMaxWidth(70);
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<TicketItem, Integer> seatCol = new TableColumn<>("Seat");
        seatCol.setMaxWidth(70);
        seatCol.setCellValueFactory(new PropertyValueFactory<>("seat"));

        TableColumn<TicketItem, String> notesCol = new TableColumn<>("Notes / modifiers");
        notesCol.setCellValueFactory(cd -> Bindings.createStringBinding(() -> {
            String n = cd.getValue().notes();
            return n == null || n.isBlank() ? "—" : n;
        }));

        TableColumn<TicketItem, String> unitCol = new TableColumn<>("Price");
        unitCol.setMaxWidth(100);
        unitCol.setCellValueFactory(cd -> Bindings.createStringBinding(() ->
                formatMoney(cd.getValue().menuItem().price())));

        TableColumn<TicketItem, String> lineCol = new TableColumn<>("Line total");
        lineCol.setMaxWidth(120);
        lineCol.setCellValueFactory(cd -> Bindings.createStringBinding(() ->
                formatMoney(cd.getValue().lineTotal())));

        TableColumn<TicketItem, String> paidCol = new TableColumn<>("Paid");
        paidCol.setMaxWidth(90);
        paidCol.setCellValueFactory(cd -> Bindings.createStringBinding(() ->
                cd.getValue().paidQuantity() + " / " + cd.getValue().quantity(),
                cd.getValue().paidQuantityProperty(),
                cd.getValue().quantityProperty()));

        TableColumn<TicketItem, String> rfdCol = new TableColumn<>("Refund");
        rfdCol.setMaxWidth(100);
        rfdCol.setCellValueFactory(cd -> Bindings.createStringBinding(() -> {
            int rq = cd.getValue().refundedQuantity();
            return rq <= 0 ? "—" : "Refunded" + (rq > 1 ? " (" + rq + ")" : "");
        }, cd.getValue().refundedQuantityProperty()));

        tv.getColumns().addAll(List.of(nameCol, qtyCol, seatCol, notesCol, unitCol, lineCol, paidCol, rfdCol));
        return tv;
    }

    private VBox buildRightPanel() {
        Label sumTitle = new Label("Summary");
        sumTitle.getStyleClass().add("checkout-section-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.getStyleClass().add("checkout-summary-box");
        ColumnConstraints c1 = new ColumnConstraints();
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setMinWidth(120);
        c2.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().addAll(c1, c2);
        int r = 0;
        grid.add(new Label("Subtotal"), 0, r);
        grid.add(subtotalVal, 1, r++);
        grid.add(new Label("Tax (8%)"), 0, r);
        grid.add(taxVal, 1, r++);
        grid.add(new Label("Discount / comp"), 0, r);
        HBox discRow = new HBox(8, discountVal, lockedPill("Manager"));
        discRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(discRow, 1, r++);
        grid.add(new Separator(), 0, r++, 2, 1);
        grid.add(new Label("Food & tax"), 0, r);
        grid.add(totalFoodVal, 1, r++);
        grid.add(new Label("Paid"), 0, r);
        grid.add(paidVal, 1, r++);
        grid.add(new Label("Balance due"), 0, r);
        balanceVal.getStyleClass().add("checkout-balance-due");
        grid.add(balanceVal, 1, r);

        cashToggle.setToggleGroup(payTypeGroup);
        cardToggle.setToggleGroup(payTypeGroup);
        cashToggle.setUserData(PaymentMethod.CASH);
        cardToggle.setUserData(PaymentMethod.CARD);
        cashToggle.getStyleClass().addAll("touch-btn", "touch-toggle");
        cardToggle.getStyleClass().addAll("touch-btn", "touch-toggle");
        cashToggle.setSelected(true);
        HBox typeRow = new HBox(10, cashToggle, cardToggle);
        typeRow.setAlignment(Pos.CENTER_LEFT);

        Label payTitle = new Label("Payment");
        payTitle.getStyleClass().add("checkout-section-title");

        payAmountField.getStyleClass().add("checkout-money-field");
        tenderedField.getStyleClass().add("checkout-money-field");

        Label payAmtLbl = new Label("Custom payment amount (split / partial)");
        payAmtLbl.getStyleClass().add("checkout-field-label");
        HBox payRow = new HBox(12, payAmtLbl, payAmountField);
        payRow.setAlignment(Pos.CENTER_LEFT);

        Button payFull = new Button("Pay full balance");
        payFull.getStyleClass().addAll("touch-btn", "touch-btn-lg", "btn-primary", "checkout-pay-full");
        payFull.setMaxWidth(Double.MAX_VALUE);
        payFull.setOnAction(e -> {
            clearMessage();
            payAmountField.setText(orders.remainingTotal(ticket).toPlainString());
            setMessage("Amount set to full balance. Confirm payment below.", false);
        });

        Button confirmPay = new Button("Confirm payment");
        confirmPay.getStyleClass().addAll("touch-btn", "touch-btn-lg", "btn-primary");
        confirmPay.setMaxWidth(Double.MAX_VALUE);
        confirmPay.setOnAction(e -> doConfirmPayment());
        Runnable syncConfirmLabel = () ->
                confirmPay.setText(cashToggle.isSelected() ? "Confirm cash payment" : "Confirm card payment");
        syncConfirmLabel.run();
        cashToggle.selectedProperty().addListener((o, a, n) -> syncConfirmLabel.run());
        cardToggle.selectedProperty().addListener((o, a, n) -> syncConfirmLabel.run());

        Label splitTitle = new Label("Split evenly (sets amount to 1st share)");
        splitTitle.getStyleClass().add("checkout-field-label");
        Button b2 = splitChip("÷ 2", 2);
        Button b3 = splitChip("÷ 3", 3);
        Button b4 = splitChip("÷ 4", 4);
        HBox evenRow = new HBox(10, b2, b3, b4);

        Label seatTitle = new Label("Split by seat");
        seatTitle.getStyleClass().add("checkout-field-label");
        seatSplitBox.setAlignment(Pos.CENTER_LEFT);
        VBox seatBlock = new VBox(6, seatTitle, seatSplitBox);

        cashBox.getChildren().setAll(
                labeledRow("Cash tendered", tenderedField),
                new HBox(16, new Label("Change due"), changeLabel)
        );
        cashBox.setVisible(true);
        cashBox.setManaged(true);

        Button printReceiptBtn = receiptActionButton("Print receipt");
        printReceiptBtn.setOnAction(e -> onPrintReceipt());
        Button emailReceiptBtn = receiptActionButton("Email receipt");
        emailReceiptBtn.setOnAction(e -> onEmailReceipt());
        HBox stubRow = new HBox(10, printReceiptBtn, emailReceiptBtn);
        stubRow.setAlignment(Pos.CENTER_LEFT);

        Button compBtn = stubLocked("Comp");
        Button refundBtn = stubLocked("Refund");
        HBox mgrRow = new HBox(10, compBtn, refundBtn);

        finalizeBtn.getStyleClass().addAll("touch-btn", "touch-btn-lg", "checkout-finalize");
        finalizeBtn.setMaxWidth(Double.MAX_VALUE);
        finalizeBtn.setOnAction(e -> {
            if (orders.remainingTotal(ticket).signum() > 0) {
                setMessage("Cannot close ticket until balance is $0.00.", true);
                return;
            }
            setMessage("Check is paid. Table can be marked dirty from the floor plan.", false);
            stage.close();
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll("touch-btn", "checkout-cancel");
        cancel.setOnAction(e -> stage.close());
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        HBox bottomActions = new HBox(12, cancel, bottomSpacer, finalizeBtn);
        bottomActions.setAlignment(Pos.CENTER_LEFT);
        finalizeBtn.setMinWidth(200);

        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(400);
        msgLabel.getStyleClass().add("checkout-message");
        VBox msgWrap = new VBox(msgLabel);
        msgWrap.setPadding(new Insets(4, 0, 0, 0));

        Separator sep = new Separator();

        payAmountField.setText(orders.remainingTotal(ticket).toPlainString());

        return new VBox(14,
                sumTitle, grid,
                sep,
                payTitle, typeRow, payFull, payRow,
                cashBox,
                confirmPay,
                splitTitle, evenRow,
                seatBlock,
                stubRow, mgrRow,
                msgWrap,
                bottomActions
        );
    }

    private Button splitChip(String text, int parts) {
        Button b = new Button(text);
        b.getStyleClass().addAll("touch-btn", "checkout-split-chip");
        b.setOnAction(e -> {
            clearMessage();
            BigDecimal rem = orders.remainingTotal(ticket);
            if (rem.signum() <= 0) {
                setMessage("No balance left to split.", true);
                return;
            }
            BigDecimal[] shares = OrderService.equalSplitParts(rem, parts);
            payAmountField.setText(shares[0].toPlainString());
            setMessage("Pay each guest this amount, then tap Confirm payment for each. Part 1 shown; last part includes extra cents.", false);
            evenSplitMode = true;
            seatSplitBox.setDisable(true);
        });
        return b;
    }

    private void rebuildSeatButtons() {
        seatSplitBox.getChildren().clear();
        List<Integer> seats = ticket.items().stream()
                .filter(i -> i.unpaidQuantity() > 0)
                .map(TicketItem::seat)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (seats.isEmpty()) {
            Label empty = new Label("No unpaid seats.");
            empty.getStyleClass().add("muted");
            seatSplitBox.getChildren().add(empty);
            return;
        }
        for (int seat : seats) {
            BigDecimal due = orders.unpaidSeatTotalWithTax(ticket, seat);
            if (due.signum() <= 0) continue;
            Button b = new Button("Pay seat " + seat + "\n" + formatMoney(due));
            b.getStyleClass().addAll("touch-btn", "checkout-seat-btn");
            b.setWrapText(true);
            final int s = seat;
            b.setOnAction(ev -> paySeat(s));
            seatSplitBox.getChildren().add(b);
        }
    }

    private void paySeat(int seat) {
        clearMessage();
        PaymentMethod method = cashToggle.isSelected() ? PaymentMethod.CASH : PaymentMethod.CARD;
        BigDecimal target = orders.unpaidSeatTotalWithTax(ticket, seat);
        if (target.signum() <= 0) {
            setMessage("No unpaid balance for that seat.", true);
            return;
        }
        if (method == PaymentMethod.CASH) {
            BigDecimal tendered;
            try {
                tendered = new BigDecimal(tenderedField.getText().trim()).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception ex) {
                setMessage("Enter cash tendered, or switch to Card.", true);
                return;
            }
            if (tendered.compareTo(target) < 0) {
                setMessage("Tendered must cover seat total (" + formatMoney(target) + ").", true);
                return;
            }
        }
        try {
            orders.recordFullSeatPayment(ticket, seat, method);
        } catch (RuntimeException ex) {
            setMessage(ex.getMessage(), true);
            return;
        }
        BigDecimal rem = orders.remainingTotal(ticket);
        payAmountField.setText(rem.toPlainString());
        if (rem.signum() <= 0) {
            setMessage("Payment successful. Balance paid. Tap Close check, then mark the table dirty on the floor plan.", false);
        } else {
            setMessage("Seat " + seat + " paid. Remaining balance: " + formatMoney(rem), false);
        }
        refreshAll();
    }

    private void doConfirmPayment() {
        clearMessage();
        PaymentMethod method = cashToggle.isSelected() ? PaymentMethod.CASH : PaymentMethod.CARD;
        BigDecimal payAmt;
        try {
            payAmt = new BigDecimal(payAmountField.getText().trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            setMessage("Invalid payment amount.", true);
            return;
        }
        if (payAmt.signum() <= 0) {
            setMessage("Enter an amount greater than zero.", true);
            return;
        }
        if (method == PaymentMethod.CASH) {
            BigDecimal tendered;
            try {
                tendered = new BigDecimal(tenderedField.getText().trim()).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception ex) {
                setMessage("Enter cash tendered, or switch to Card.", true);
                return;
            }
            if (tendered.compareTo(payAmt) < 0) {
                setMessage("Tendered must cover payment (" + formatMoney(payAmt) + ").", true);
                return;
            }
        }
        try {
            orders.recordAmountPayment(ticket, payAmt, method);
        } catch (RuntimeException ex) {
            setMessage(ex.getMessage(), true);
            return;
        }
        BigDecimal rem = orders.remainingTotal(ticket);
        payAmountField.setText(rem.toPlainString());
        if (rem.signum() <= 0) {
            setMessage("Payment successful. Balance paid. Tap Close check, then mark the table dirty on the floor plan.", false);
        } else {
            setMessage("Payment recorded. Remaining balance: " + formatMoney(rem), false);
        }
        refreshAll();
    }

    private void refreshAll() {
        BigDecimal sub = ticket.subtotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = orders.tax(ticket.subtotal());
        BigDecimal foodTax = orders.ticketTotalWithTax(ticket);
        BigDecimal paid = orders.paidTotal(ticket);
        BigDecimal rem = orders.remainingTotal(ticket);

        subtotalVal.setText(formatMoney(sub));
        taxVal.setText(formatMoney(tax));
        totalFoodVal.setText(formatMoney(foodTax));
        paidVal.setText(formatMoney(paid));
        balanceVal.setText(formatMoney(rem));

        updateStatusBadge(rem, paid);

        finalizeBtn.setDisable(rem.signum() > 0);
        payAmountField.setDisable(ticket.status() == OrderStatus.PAID);
        cashToggle.setDisable(ticket.status() == OrderStatus.PAID);
        cardToggle.setDisable(ticket.status() == OrderStatus.PAID);
        rebuildSeatButtons();
        seatSplitBox.setDisable(evenSplitMode);
        refreshChangeOnly();
    }

    private void refreshChangeOnly() {
        if (!cashToggle.isSelected()) {
            changeLabel.setText("—");
            return;
        }
        try {
            BigDecimal tender = new BigDecimal(tenderedField.getText().trim());
            BigDecimal pay = new BigDecimal(payAmountField.getText().trim());
            BigDecimal ch = tender.subtract(pay).setScale(2, RoundingMode.HALF_UP);
            if (ch.signum() < 0) ch = BigDecimal.ZERO;
            changeLabel.setText(formatMoney(ch));
        } catch (Exception e) {
            changeLabel.setText("—");
        }
    }

    private void updateStatusBadge(BigDecimal remaining, BigDecimal paid) {
        if (ticket.status() == OrderStatus.PAID || remaining.signum() <= 0) {
            statusBadge.setText("Status: Paid");
            statusBadge.getStyleClass().setAll("checkout-status-badge", "checkout-status-paid");
        } else if (paid.signum() > 0) {
            statusBadge.setText("Status: Partially paid");
            statusBadge.getStyleClass().setAll("checkout-status-badge", "checkout-status-partial");
        } else {
            statusBadge.setText("Status: Unpaid");
            statusBadge.getStyleClass().setAll("checkout-status-badge", "checkout-status-unpaid");
        }
    }

    private HBox labeledRow(String name, TextField field) {
        Label l = new Label(name);
        l.getStyleClass().add("checkout-field-label");
        HBox row = new HBox(12, l, field);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button receiptActionButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("touch-btn", "checkout-secondary");
        return b;
    }

    private void onPrintReceipt() {
        AlertHelper.info("Receipt", "Order #" + ticket.orderNumber() + " · Table " + ticket.tableId()
                + " · " + ticket.guestCount() + " guests\nReceipt printed.");
    }

    private void onEmailReceipt() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.initOwner(stage);
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

    private Button stubLocked(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("touch-btn", "checkout-locked");
        b.setOnAction(e -> AlertHelper.info("Manager approval", "This action requires manager approval."));
        return b;
    }

    private Region lockedPill(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("checkout-pill");
        return l;
    }

    private void setMessage(String text, boolean error) {
        msgLabel.setText(text);
        msgLabel.getStyleClass().setAll("checkout-message");
        if (error) {
            msgLabel.getStyleClass().add("error");
        } else {
            msgLabel.getStyleClass().add("success");
        }
    }

    private void clearMessage() {
        msgLabel.setText(" ");
        msgLabel.getStyleClass().setAll("checkout-message");
    }

    private static BigDecimal parseMoney(String s, BigDecimal def) {
        if (s == null || s.isBlank()) return def;
        try {
            return new BigDecimal(s.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return def;
        }
    }

    private static String formatMoney(BigDecimal v) {
        return "$" + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatTicketNo(String id) {
        if (id == null || id.length() < 4) return "#—";
        String compact = id.replace("-", "");
        String tail = compact.length() <= 4 ? compact : compact.substring(compact.length() - 4);
        return "#" + tail.toUpperCase();
    }

    private String resolveServerLabel() {
        Employee u = AppContext.session().currentUser();
        if (u != null && u.employeeId().equalsIgnoreCase(ticket.waiterId())) {
            return u.displayName() + " (" + u.employeeId() + ")";
        }
        return ticket.waiterId();
    }

    private static ImageView smallLogo() {
        var stream = CheckoutScreen.class.getResourceAsStream("/com/jjcorner/view/jj-logo.png");
        ImageView iv = new ImageView(stream != null ? new Image(stream) : null);
        iv.setFitHeight(40);
        iv.setFitWidth(40);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }
}
