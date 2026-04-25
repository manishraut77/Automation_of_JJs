package com.jjcorner.app.service;

import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.MenuItem;
import com.jjcorner.app.model.OrderStatus;
import com.jjcorner.app.model.Payment;
import com.jjcorner.app.model.PaymentMethod;
import com.jjcorner.app.model.RestaurantTable;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.model.TableStatus;
import com.jjcorner.app.model.Ticket;
import com.jjcorner.app.model.TicketItem;
import com.jjcorner.app.persist.TicketLedger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates ticket creation, order entry, kitchen submission, payments, and refunds.
 */
public final class OrderService {
    public static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private final SessionManager session;
    private final TableService tables;
    private final MenuService menu;
    private final InventoryService inventory;
    private final ActivityService activity;
    private final ObservableList<Ticket> realTickets = FXCollections.observableArrayList();
    private final AtomicInteger realNextOrderNumber = new AtomicInteger(1000);
    private final Map<String, ObservableList<Ticket>> demoTicketsByEmployeeId = new HashMap<>();
    private final Map<String, AtomicInteger> demoNextOrderNumberByEmployeeId = new HashMap<>();

    public OrderService(SessionManager session, TableService tables, MenuService menu, InventoryService inventory, ActivityService activity) {
        this.session = session;
        this.tables = tables;
        this.menu = menu;
        this.inventory = inventory;
        this.activity = activity;
        TicketLedger.Loaded loaded = TicketLedger.load(menu);
        realTickets.addAll(loaded.tickets());
        realNextOrderNumber.set(loaded.lastAssignedOrderNumber());
        persist();
    }

    public ObservableList<Ticket> allTickets() {
        return activeTickets();
    }

    public ObservableList<Ticket> ticketsForCurrentWaiter() {
        Employee u = session.currentUser();
        if (u == null || u.role() != Role.WAITER) return FXCollections.observableArrayList();
        return activeTickets().filtered(t -> t.waiterId().equals(u.employeeId()))
                .sorted(Comparator.comparing(Ticket::tableId));
    }

    /**
     * All checks that have been closed at least once (including partial refunds / reopened balance), newest first.
     */
    public List<Ticket> closedCheckHistoryForCurrentWaiter() {
        Employee u = session.currentUser();
        if (u == null || (u.role() != Role.WAITER && u.role() != Role.MANAGER)) return List.of();
        return activeTickets().stream()
                .filter(t -> u.role() == Role.MANAGER || t.waiterId().equals(u.employeeId()))
                .filter(Ticket::inClosedHistory)
                .sorted(Comparator.comparing(Ticket::createdAt).reversed())
                .toList();
    }

    public boolean mayOpenClosedCheckDialog(Ticket ticket) {
        if (ticket == null || !ticket.inClosedHistory()) {
            return false;
        }
        if (ticket.status() == OrderStatus.PAID) {
            return true;
        }
        return ticket.status() == OrderStatus.PENDING && paidTotal(ticket).compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean ticketHasRefunds(Ticket ticket) {
        if (ticket == null) {
            return false;
        }
        return ticket.payments().stream().anyMatch(p -> p.amount().signum() < 0);
    }

    public static boolean isGhostDraftTicket(Ticket t) {
        return t.status() == OrderStatus.DRAFT && t.items().isEmpty() && t.payments().isEmpty();
    }

    /**
     * Paid tickets are closed. Empty unpaid drafts do not count (prevents ghost checks after checkout).
     */
    public static boolean countsAsOpenCheck(Ticket t) {
        if (t.status() == OrderStatus.PAID) return false;
        return !isGhostDraftTicket(t);
    }

    public boolean currentWaiterHasOpenChecks() {
        Employee u = session.currentUser();
        if (u == null || u.role() != Role.WAITER) return false;
        return activeTickets().stream()
                .filter(t -> t.waiterId().equals(u.employeeId()))
                .anyMatch(OrderService::countsAsOpenCheck);
    }

    /**
     * Removes empty DRAFT tickets with no payments for a table (leftover shells after closing checkout).
     */
    public void removeGhostDraftTicketsForTable(String tableId) {
        if (tableId == null) return;
        String canonicalTableId = tables.primaryTableFor(tableId).id();
        activeTickets().removeIf(t -> t.tableId().equalsIgnoreCase(canonicalTableId) && isGhostDraftTicket(t));
        persist();
    }

    public Ticket openOrCreateForTable(String tableId) {
        Employee u = requireWaiter();
        requireClockedIn();
        RestaurantTable selected = tables.requireTable(tableId);
        RestaurantTable t = tables.primaryTableFor(selected);
        String canonicalTableId = t.id();
        if (!tables.groupAssignedTo(t, u.employeeId()) && u.role()==Role.WAITER) {
            throw new IllegalArgumentException("Error: Table is not assigned to you");
        }
        if (t.status() != TableStatus.OCCUPIED) {
            throw new IllegalArgumentException("Error: Orders can only be created for occupied tables");
        }

        removeGhostDraftTicketsForTable(canonicalTableId);

        ObservableList<Ticket> list = activeTickets();
        Optional<Ticket> existing = list.stream()
                .filter(x -> x.tableId().equalsIgnoreCase(canonicalTableId))
                .filter(OrderService::countsAsOpenCheck)
                .findFirst();
        if (existing.isPresent()) return existing.get();

        int guests = Math.max(1, t.guestCount());
        int orderNo = activeNextOrderNumber().incrementAndGet();
        Ticket created = new Ticket(u.employeeId(), canonicalTableId, orderNo, guests);
        list.add(created);
        activity.record(u, "Opened order #" + orderNo + " for table " + tables.displayIdForGroup(t));
        persist();
        return created;
    }

    /**
     * Latest ticket for a table by time, ignoring empty ghost drafts so a paid check stays “latest”.
     */
    public Optional<Ticket> latestTicketForTable(String tableId) {
        if (tableId == null) return Optional.empty();
        tableId = tables.primaryTableFor(tableId).id();
        String canonicalTableId = tableId;
        return activeTickets().stream()
                .filter(t -> t.tableId().equalsIgnoreCase(canonicalTableId))
                .filter(t -> !isGhostDraftTicket(t))
                .max(Comparator.comparing(Ticket::createdAt));
    }

    public TicketItem addItem(Ticket ticket, MenuItem item, int seat, int quantity, String notes) {
        return addItem(ticket, item, seat, quantity, notes, BigDecimal.ZERO);
    }

    public TicketItem addItem(Ticket ticket, MenuItem item, int seat, int quantity, String notes, BigDecimal unitExtraPerUnit) {
        Objects.requireNonNull(ticket);
        Objects.requireNonNull(item);
        requireWaiter();
        requireClockedIn();

        if (ticket.status() == OrderStatus.PAID) {
            throw new IllegalArgumentException("Error: Cannot modify a paid ticket");
        }
        TicketItem ti = new TicketItem(item);
        ti.setSeat(Math.max(1, Math.min(4, seat)));
        ti.setQuantity(Math.max(1, quantity));
        ti.setNotes(notes);
        ti.setUnitExtra(unitExtraPerUnit == null ? BigDecimal.ZERO : unitExtraPerUnit);
        ticket.items().add(ti);
        activity.record(session.currentUser(), "Added " + ti.quantity() + " " + item.name() + " to order #" + ticket.orderNumber());
        persist();
        return ti;
    }

    public void submitToKitchen(Ticket ticket) {
        Objects.requireNonNull(ticket);
        requireWaiter();
        requireClockedIn();
        if (session.isDemoMode()) {
            // Demo mode: do not send anything to kitchen or mutate sent flags.
            return;
        }
        if (ticket.status() == OrderStatus.PAID) {
            throw new IllegalArgumentException("Error: Cannot submit changes on a paid ticket");
        }
        boolean hasUnsent = ticket.items().stream().anyMatch(i -> !i.isSentToKitchen());
        if (!hasUnsent) {
            throw new IllegalArgumentException("Error: No new items to send to kitchen");
        }
        for (TicketItem it : ticket.items()) {
            if (!it.isSentToKitchen()) {
                inventory.consumeForMenuItem(it.menuItem(), it.quantity());
                it.setSentToKitchen(true);
            }
        }
        if (ticket.status() == OrderStatus.DRAFT) {
            ticket.setStatus(OrderStatus.PENDING);
        }
        activity.record(session.currentUser(), "Submitted order #" + ticket.orderNumber() + " to kitchen");
        persist();
    }

    public void removeItem(Ticket ticket, TicketItem item) {
        Objects.requireNonNull(ticket);
        Objects.requireNonNull(item);
        requireWaiter();
        requireClockedIn();
        if (ticket.status() == OrderStatus.PAID) {
            throw new IllegalArgumentException("Error: Cannot modify a paid ticket");
        }
        if (item.isSentToKitchen()) {
            throw new IllegalArgumentException("Error: Cannot remove items already sent to kitchen");
        }
        ticket.items().remove(item);
        activity.record(session.currentUser(), "Removed " + item.menuItem().name() + " from order #" + ticket.orderNumber());
        persist();
    }

    public BigDecimal ticketTotalWithTax(Ticket ticket) {
        return total(ticket.subtotal());
    }

    public BigDecimal ticketAmountDue(Ticket ticket) {
        return ticketTotalWithTax(ticket).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal paidTotal(Ticket ticket) {
        return ticket.payments().stream()
                .map(Payment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal remainingTotal(Ticket ticket) {
        BigDecimal remaining = ticketAmountDue(ticket).subtract(paidTotal(ticket));
        if (remaining.signum() < 0) remaining = BigDecimal.ZERO;
        return remaining.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Sum of unpaid line subtotals for rows marked with the checkbox (split selection).
     */
    public BigDecimal selectedUnpaidSubtotal(Ticket ticket) {
        return ticket.items().stream()
                .filter(TicketItem::selectedForPayment)
                .map(TicketItem::unpaidSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tax-included total for selected unpaid lines (used for split-by-selection).
     */
    public BigDecimal selectedUnpaidTotalWithTax(Ticket ticket) {
        return total(selectedUnpaidSubtotal(ticket));
    }

    /**
     * Split {@code total} into {@code parts} equal money amounts; leftover cents go to the last part.
     */
    public static BigDecimal[] equalSplitParts(BigDecimal total, int parts) {
        if (parts <= 0) throw new IllegalArgumentException("parts must be positive");
        total = total.setScale(2, RoundingMode.HALF_UP);
        if (total.signum() <= 0) {
            BigDecimal[] z = new BigDecimal[parts];
            for (int i = 0; i < parts; i++) z[i] = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return z;
        }
        long cents = total.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        long q = cents / parts;
        long r = cents % parts;
        BigDecimal[] out = new BigDecimal[parts];
        for (int i = 0; i < parts; i++) {
            long c = q + (i == parts - 1 ? r : 0);
            out[i] = BigDecimal.valueOf(c).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        }
        return out;
    }

    public void recordAmountPayment(Ticket ticket, BigDecimal amount, PaymentMethod method) {
        Objects.requireNonNull(ticket);
        Objects.requireNonNull(method);
        requireWaiter();
        requireClockedIn();
        if (ticket.status() == OrderStatus.PAID) {
            throw new IllegalArgumentException("Error: Ticket is already paid");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Error: Payment amount must be greater than 0");
        }
        if (method != PaymentMethod.CASH && method != PaymentMethod.CARD) {
            throw new IllegalArgumentException("Error: Invalid payment method for this action");
        }
        BigDecimal remaining = remainingTotal(ticket);
        BigDecimal amt = amount.setScale(2, RoundingMode.HALF_UP);
        if (amt.compareTo(remaining) > 0) {
            amt = remaining; // cap to remaining
        }
        ticket.payments().add(new Payment(amt, method));
        if (remainingTotal(ticket).compareTo(BigDecimal.ZERO) == 0) {
            for (TicketItem it : ticket.items()) {
                it.setPaidQuantity(it.quantity());
            }
            ticket.setStatus(OrderStatus.PAID);
            ticket.setInClosedHistory(true);
            removeGhostDraftTicketsForTable(ticket.tableId());
        }
        activity.record(session.currentUser(), "Recorded " + method + " payment of $" + amt + " for order #" + ticket.orderNumber());
        persist();
    }

    public void recordItemQuantityPayment(Ticket ticket, TicketItem item, int qtyToPay, PaymentMethod method) {
        Objects.requireNonNull(ticket);
        Objects.requireNonNull(item);
        Objects.requireNonNull(method);
        requireWaiter();
        requireClockedIn();
        if (ticket.status() == OrderStatus.PAID) {
            throw new IllegalArgumentException("Error: Ticket is already paid");
        }
        int unpaid = item.unpaidQuantity();
        if (unpaid <= 0) {
            throw new IllegalArgumentException("Error: Item is already fully paid");
        }
        int qty = Math.max(1, Math.min(unpaid, qtyToPay));

        // charge item subtotal + proportional tax for this portion
        BigDecimal portionSubtotal = item.effectiveUnitPrice().multiply(BigDecimal.valueOf(qty));
        BigDecimal portionTotal = total(portionSubtotal);
        recordAmountPayment(ticket, portionTotal, method);

        item.setPaidQuantity(item.paidQuantity() + qty);
    }

    public BigDecimal tax(BigDecimal subtotal) {
        return subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal total(BigDecimal subtotal) {
        return subtotal.add(tax(subtotal)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Unpaid line subtotals for items on {@code seat} (1–4).
     */
    public BigDecimal unpaidSeatSubtotal(Ticket ticket, int seat) {
        return ticket.items().stream()
                .filter(i -> i.seat() == seat)
                .map(TicketItem::unpaidSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tax-included unpaid total for one seat (split-by-seat payments).
     */
    public BigDecimal unpaidSeatTotalWithTax(Ticket ticket, int seat) {
        return total(unpaidSeatSubtotal(ticket, seat));
    }

    /**
     * Records one payment for the full unpaid balance of {@code seat} and marks all lines on that seat as paid.
     */
    public void recordFullSeatPayment(Ticket ticket, int seat, PaymentMethod method) {
        Objects.requireNonNull(ticket);
        Objects.requireNonNull(method);
        requireWaiter();
        requireClockedIn();
        if (ticket.status() == OrderStatus.PAID) {
            throw new IllegalArgumentException("Error: Ticket is already paid");
        }
        BigDecimal target = unpaidSeatTotalWithTax(ticket, seat);
        if (target.signum() <= 0) {
            throw new IllegalArgumentException("Error: No unpaid balance for that seat");
        }
        BigDecimal remaining = remainingTotal(ticket);
        if (remaining.compareTo(target) < 0) {
            throw new IllegalArgumentException("Error: Remaining balance is less than this seat total. Pay the open balance first.");
        }
        recordAmountPayment(ticket, target, method);
        for (TicketItem it : ticket.items()) {
            if (it.seat() == seat) {
                it.setPaidQuantity(it.quantity());
            }
        }
    }

    public void markPaid(Ticket ticket) {
        Objects.requireNonNull(ticket);
        requireWaiter();
        requireClockedIn();
        for (TicketItem it : ticket.items()) {
            it.setPaidQuantity(it.quantity());
        }
        ticket.setStatus(OrderStatus.PAID);
        ticket.setInClosedHistory(true);
        activity.record(session.currentUser(), "Marked order #" + ticket.orderNumber() + " paid");
        persist();
    }

    /**
     * Paid tickets from “pay balance” paths should have each line’s paid qty match quantity.
     * Repairs older in-memory tickets where only payments were recorded.
     */
    public void syncLinePaidQuantitiesForPaidTicket(Ticket ticket) {
        Objects.requireNonNull(ticket);
        if (ticket.status() != OrderStatus.PAID || ticket.items().isEmpty()) {
            return;
        }
        if (ticket.items().stream().anyMatch(it -> it.paidQuantity() > 0)) {
            return;
        }
        for (TicketItem it : ticket.items()) {
            it.setPaidQuantity(it.quantity());
        }
        ticket.setInClosedHistory(true);
        persist();
    }

    /**
     * Refunds the paid portion of each selected line (full paid quantity on that line) on a paid ticket.
     * Records a negative payment and reopens the ticket as {@link OrderStatus#PENDING} if a balance remains.
     */
    public void recordRefundForPaidLines(Ticket ticket, List<TicketItem> linesToRefund, PaymentMethod method) {
        Objects.requireNonNull(ticket);
        Objects.requireNonNull(method);
        requireWaiter();
        requireClockedIn();
        if (!ticket.inClosedHistory()) {
            throw new IllegalArgumentException("Error: Refunds here are only for checks in closed history");
        }
        if (ticket.status() != OrderStatus.PAID && ticket.status() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Error: Cannot refund this check in its current state");
        }
        BigDecimal netPaidBefore = paidTotal(ticket);
        if (netPaidBefore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Error: No net payments to refund against");
        }
        if (linesToRefund == null || linesToRefund.isEmpty()) {
            throw new IllegalArgumentException("Error: Select at least one item to refund");
        }
        List<TicketItem> lines = linesToRefund.stream().distinct().toList();
        BigDecimal refundTotal = BigDecimal.ZERO;
        for (TicketItem it : lines) {
            if (!ticket.items().contains(it)) {
                throw new IllegalArgumentException("Error: Invalid line item for this ticket");
            }
            int pq = it.paidQuantity();
            if (pq <= 0) {
                continue;
            }
            BigDecimal portionSub = it.effectiveUnitPrice().multiply(BigDecimal.valueOf(pq));
            refundTotal = refundTotal.add(total(portionSub));
        }
        refundTotal = refundTotal.setScale(2, RoundingMode.HALF_UP);
        if (refundTotal.signum() <= 0) {
            throw new IllegalArgumentException("Error: Nothing to refund on the selected lines");
        }
        if (refundTotal.compareTo(netPaidBefore) > 0) {
            throw new IllegalArgumentException("Error: Refund exceeds amount paid on this check");
        }
        for (TicketItem it : lines) {
            int pq = it.paidQuantity();
            if (pq > 0) {
                it.setRefundedQuantity(it.refundedQuantity() + pq);
                it.setPaidQuantity(0);
            }
        }
        ticket.payments().add(new Payment(refundTotal.negate(), method));

        // Refunds should not reopen the check. After refunding paid lines, we auto-settle any remaining
        // balance to keep the ticket closed in history.
        BigDecimal rem = remainingTotal(ticket);
        if (rem.compareTo(BigDecimal.ZERO) > 0) {
            ticket.payments().add(new Payment(rem, PaymentMethod.ADJUSTMENT));
        }
        ticket.setStatus(OrderStatus.PAID);
        activity.record(session.currentUser(), "Issued refund of $" + refundTotal + " on order #" + ticket.orderNumber());
        persist();
    }

    private void persist() {
        if (session.isDemoMode()) {
            return;
        }
        TicketLedger.save(new ArrayList<>(realTickets), realNextOrderNumber);
    }

    private ObservableList<Ticket> activeTickets() {
        if (!session.isDemoMode()) {
            return realTickets;
        }
        Employee u = session.currentUser();
        String key = u == null ? "anon" : u.employeeId();
        return demoTicketsByEmployeeId.computeIfAbsent(key, k -> FXCollections.observableArrayList());
    }

    private AtomicInteger activeNextOrderNumber() {
        if (!session.isDemoMode()) {
            return realNextOrderNumber;
        }
        Employee u = session.currentUser();
        String key = u == null ? "anon" : u.employeeId();
        return demoNextOrderNumberByEmployeeId.computeIfAbsent(key, k -> new AtomicInteger(9000));
    }

    public void resetDemoForCurrentUser() {
        if (!session.isDemoMode()) {
            return;
        }
        Employee u = session.currentUser();
        String key = u == null ? "anon" : u.employeeId();
        demoTicketsByEmployeeId.remove(key);
        demoNextOrderNumberByEmployeeId.remove(key);
    }

    private Employee requireWaiter() {
        Employee u = session.currentUser();
        if (u == null) {
            throw new IllegalStateException("Not logged in");
        }
        if (u.role() == Role.WAITER || u.role() == Role.MANAGER) {
            return u;
        }
        throw new IllegalStateException("Only waiters and managers can manage orders");
    }

    private void requireClockedIn() {
        if (!session.isClockedIn() && session.roleOrNull() != Role.MANAGER) {
            throw new IllegalStateException("Error: You are not clocked in");
        }
    }
}
