package com.jjcorner.app.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

/**
 * Table state shown on the floor plan, including seats, waiter assignment, and joins.
 */
public final class RestaurantTable {
    private final StringProperty id = new SimpleStringProperty();
    private final ObjectProperty<TableStatus> status = new SimpleObjectProperty<>(TableStatus.OPEN);
    private final StringProperty assignedWaiterId = new SimpleStringProperty();
    /** Party size when seated; reset when table returns to Open. */
    private final IntegerProperty guestCount = new SimpleIntegerProperty(0);
    private final IntegerProperty seatCount = new SimpleIntegerProperty(4);
    private final StringProperty joinedTableIds = new SimpleStringProperty("");

    public RestaurantTable(String id) {
        this.id.set(Objects.requireNonNull(id));
    }

    public String id() {
        return id.get();
    }

    public StringProperty idProperty() {
        return id;
    }

    public TableStatus status() {
        return status.get();
    }

    public void setStatus(TableStatus newStatus) {
        status.set(newStatus);
    }

    public ObjectProperty<TableStatus> statusProperty() {
        return status;
    }

    public String assignedWaiterId() {
        return assignedWaiterId.get();
    }

    public void setAssignedWaiterId(String waiterId) {
        assignedWaiterId.set(waiterId);
    }

    public StringProperty assignedWaiterIdProperty() {
        return assignedWaiterId;
    }

    public boolean isAssignedTo(String waiterId) {
        return waiterId != null && waiterId.equals(assignedWaiterId());
    }

    public int guestCount() {
        return guestCount.get();
    }

    public void setGuestCount(int value) {
        guestCount.set(Math.max(0, value));
    }

    public IntegerProperty guestCountProperty() {
        return guestCount;
    }

    public int seatCount() {
        return seatCount.get();
    }

    public void setSeatCount(int value) {
        seatCount.set(Math.max(1, value));
    }

    public IntegerProperty seatCountProperty() {
        return seatCount;
    }

    public String joinedTableIds() {
        return joinedTableIds.get();
    }

    public void setJoinedTableIds(String value) {
        joinedTableIds.set(value == null ? "" : value.trim());
    }

    public StringProperty joinedTableIdsProperty() {
        return joinedTableIds;
    }
}

