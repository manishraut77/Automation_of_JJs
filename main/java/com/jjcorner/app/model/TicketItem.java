package com.jjcorner.app.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.util.Objects;

public final class TicketItem {
    private final MenuItem menuItem;
    private final IntegerProperty seat = new SimpleIntegerProperty(1);
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);
    private final StringProperty notes = new SimpleStringProperty("");
    private final IntegerProperty paidQuantity = new SimpleIntegerProperty(0);
    /** Units refunded on this line (cumulative); shown as “Refunded” on checks. */
    private final IntegerProperty refundedQuantity = new SimpleIntegerProperty(0);
    private final BooleanProperty selectedForPayment = new SimpleBooleanProperty(false);
    /** After submit to kitchen; new lines stay false until next submit. */
    private final BooleanProperty sentToKitchen = new SimpleBooleanProperty(false);
    /** Added to menu unit price (e.g. BBQ sauce on nachos). */
    private final ObjectProperty<BigDecimal> unitExtra = new SimpleObjectProperty<>(BigDecimal.ZERO);

    public TicketItem(MenuItem menuItem) {
        this.menuItem = Objects.requireNonNull(menuItem);
    }

    public MenuItem menuItem() {
        return menuItem;
    }

    public int seat() {
        return seat.get();
    }

    public void setSeat(int value) {
        seat.set(value);
    }

    public IntegerProperty seatProperty() {
        return seat;
    }

    public int quantity() {
        return quantity.get();
    }

    public void setQuantity(int value) {
        quantity.set(value);
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    public String notes() {
        return notes.get();
    }

    public void setNotes(String value) {
        notes.set(value == null ? "" : value);
    }

    public StringProperty notesProperty() {
        return notes;
    }

    public int paidQuantity() {
        return paidQuantity.get();
    }

    public void setPaidQuantity(int value) {
        paidQuantity.set(Math.max(0, value));
    }

    public IntegerProperty paidQuantityProperty() {
        return paidQuantity;
    }

    public int refundedQuantity() {
        return refundedQuantity.get();
    }

    public void setRefundedQuantity(int value) {
        refundedQuantity.set(Math.max(0, value));
    }

    public IntegerProperty refundedQuantityProperty() {
        return refundedQuantity;
    }

    public int unpaidQuantity() {
        return Math.max(0, quantity() - paidQuantity());
    }

    public boolean isFullyPaid() {
        return unpaidQuantity() == 0 && quantity() > 0;
    }

    public boolean selectedForPayment() {
        return selectedForPayment.get();
    }

    public void setSelectedForPayment(boolean value) {
        selectedForPayment.set(value);
    }

    public BooleanProperty selectedForPaymentProperty() {
        return selectedForPayment;
    }

    public boolean isSentToKitchen() {
        return sentToKitchen.get();
    }

    public void setSentToKitchen(boolean value) {
        sentToKitchen.set(value);
    }

    public BooleanProperty sentToKitchenProperty() {
        return sentToKitchen;
    }

    public BigDecimal unitExtra() {
        BigDecimal v = unitExtra.get();
        return v == null ? BigDecimal.ZERO : v.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public void setUnitExtra(BigDecimal value) {
        unitExtra.set(value == null ? BigDecimal.ZERO : value.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    public ObjectProperty<BigDecimal> unitExtraProperty() {
        return unitExtra;
    }

    /** Menu price plus per-unit modifiers (BBQ add-on, etc.). */
    public BigDecimal effectiveUnitPrice() {
        return menuItem.price().add(unitExtra()).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal lineTotal() {
        return effectiveUnitPrice().multiply(BigDecimal.valueOf(quantity())).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal unpaidSubtotal() {
        return effectiveUnitPrice().multiply(BigDecimal.valueOf(unpaidQuantity())).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}

