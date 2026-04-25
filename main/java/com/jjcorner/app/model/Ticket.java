package com.jjcorner.app.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Ticket {
    private final String id;
    private final String waiterId;
    private final String tableId;
    private final int orderNumber;
    private final IntegerProperty guestCount = new SimpleIntegerProperty(1);
    private final ObjectProperty<OrderStatus> status = new SimpleObjectProperty<>(OrderStatus.DRAFT);
    private final ObservableList<TicketItem> items = FXCollections.observableArrayList();
    private final ObservableList<Payment> payments = FXCollections.observableArrayList();
    private final Instant createdAt;
    /** Once true, this check stays in closed-check history even if later reopened (e.g. partial refund). */
    private final BooleanProperty inClosedHistory = new SimpleBooleanProperty(false);
    /** Cumulative tip amount committed at checkout (included in amount due with food & tax). */
    private final ObjectProperty<BigDecimal> recordedTipTotal = new SimpleObjectProperty<>(BigDecimal.ZERO);

    public Ticket(String waiterId, String tableId, int orderNumber, int guestCount) {
        this(UUID.randomUUID().toString(), waiterId, tableId, orderNumber, guestCount, null, null, false);
    }

    /**
     * Restore a ticket from disk (same id, timestamps, and history flag).
     */
    public static Ticket restoreFromPersistence(String id, String waiterId, String tableId, int orderNumber,
                                                int guestCount, OrderStatus status, Instant createdAt,
                                                boolean inClosedHistory, BigDecimal recordedTip) {
        Ticket t = new Ticket(id, waiterId, tableId, orderNumber, guestCount, status, createdAt, inClosedHistory);
        if (recordedTip != null && recordedTip.signum() > 0) {
            t.setRecordedTipTotal(recordedTip);
        }
        return t;
    }

    private Ticket(String id, String waiterId, String tableId, int orderNumber, int guestCount,
                   OrderStatus restoredStatus, Instant restoredCreatedAt, boolean inClosedHistoryFlag) {
        this.id = Objects.requireNonNull(id);
        this.waiterId = Objects.requireNonNull(waiterId);
        this.tableId = Objects.requireNonNull(tableId);
        this.orderNumber = orderNumber;
        this.guestCount.set(Math.max(1, guestCount));
        this.createdAt = restoredCreatedAt != null ? restoredCreatedAt : Instant.now();
        if (restoredStatus != null) {
            this.status.set(restoredStatus);
        }
        this.inClosedHistory.set(inClosedHistoryFlag);
    }

    public String id() {
        return id;
    }

    public String waiterId() {
        return waiterId;
    }

    public String tableId() {
        return tableId;
    }

    public int orderNumber() {
        return orderNumber;
    }

    public int guestCount() {
        return guestCount.get();
    }

    public IntegerProperty guestCountProperty() {
        return guestCount;
    }

    public boolean inClosedHistory() {
        return inClosedHistory.get();
    }

    public void setInClosedHistory(boolean value) {
        inClosedHistory.set(value);
    }

    public BooleanProperty inClosedHistoryProperty() {
        return inClosedHistory;
    }

    public BigDecimal recordedTipTotal() {
        BigDecimal v = recordedTipTotal.get();
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    public void setRecordedTipTotal(BigDecimal value) {
        recordedTipTotal.set(value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
    }

    public void addRecordedTip(BigDecimal tip) {
        if (tip == null || tip.signum() <= 0) {
            return;
        }
        setRecordedTipTotal(recordedTipTotal().add(tip));
    }

    public ObjectProperty<BigDecimal> recordedTipTotalProperty() {
        return recordedTipTotal;
    }

    public OrderStatus status() {
        return status.get();
    }

    public void setStatus(OrderStatus newStatus) {
        status.set(newStatus);
    }

    public ObjectProperty<OrderStatus> statusProperty() {
        return status;
    }

    public ObservableList<TicketItem> items() {
        return items;
    }

    public ObservableList<Payment> payments() {
        return payments;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public BigDecimal subtotal() {
        return items.stream()
                .map(TicketItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal unpaidSubtotal() {
        return items.stream()
                .map(TicketItem::unpaidSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

