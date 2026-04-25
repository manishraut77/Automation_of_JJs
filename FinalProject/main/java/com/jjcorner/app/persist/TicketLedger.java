package com.jjcorner.app.persist;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jjcorner.app.model.MenuItem;
import com.jjcorner.app.model.OrderStatus;
import com.jjcorner.app.model.Payment;
import com.jjcorner.app.model.PaymentMethod;
import com.jjcorner.app.model.Ticket;
import com.jjcorner.app.model.TicketItem;
import com.jjcorner.app.service.MenuService;
import com.jjcorner.app.service.OrderService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Persists non-ghost tickets, payments, line items, and the order-number sequence.
 */
public final class TicketLedger {
    private static final Path DEFAULT_PATH =
            Paths.get(System.getProperty("user.home"), ".jjcorner-pos", "ticket-ledger.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private TicketLedger() {}

    public static Path ledgerPath() {
        return DEFAULT_PATH;
    }

    /** {@code lastAssignedOrderNumber} is the highest order # used (same as OrderService counter after assign). */
    public record Loaded(List<Ticket> tickets, int lastAssignedOrderNumber) {}

    public static Loaded load(MenuService menu) {
        Path path = ledgerPath();
        if (!Files.isRegularFile(path)) {
            return new Loaded(List.of(), 1000);
        }
        try {
            RootDto root = MAPPER.readValue(path.toFile(), RootDto.class);
            int fromFile = root.lastAssignedOrderNumber != null ? root.lastAssignedOrderNumber : 1000;
            List<Ticket> out = new ArrayList<>();
            if (root.tickets != null) {
                for (TicketDto td : root.tickets) {
                    Optional<Ticket> t = rebuildTicket(td, menu);
                    t.ifPresent(out::add);
                }
            }
            int maxOrder = out.stream().mapToInt(Ticket::orderNumber).max().orElse(0);
            int high = Math.max(fromFile, maxOrder);
            return new Loaded(out, high);
        } catch (Exception e) {
            return new Loaded(List.of(), 1000);
        }
    }

    public static void save(List<Ticket> tickets, AtomicInteger nextOrderNumber) {
        Path path = ledgerPath();
        try {
            Files.createDirectories(path.getParent());
            RootDto root = new RootDto();
            int maxOnTickets = tickets.stream()
                    .filter(t -> !OrderService.isGhostDraftTicket(t))
                    .mapToInt(Ticket::orderNumber)
                    .max()
                    .orElse(0);
            int high = Math.max(nextOrderNumber.get(), maxOnTickets);
            nextOrderNumber.updateAndGet(v -> Math.max(v, high));
            root.lastAssignedOrderNumber = nextOrderNumber.get();
            root.tickets = new ArrayList<>();
            for (Ticket t : tickets) {
                if (OrderService.isGhostDraftTicket(t)) {
                    continue;
                }
                root.tickets.add(toDto(t));
            }
            MAPPER.writeValue(path.toFile(), root);
        } catch (IOException ignored) {
            // best-effort persistence
        }
    }

    private static Optional<Ticket> rebuildTicket(TicketDto td, MenuService menu) {
        if (td == null || td.id == null || td.waiterId == null || td.tableId == null) {
            return Optional.empty();
        }
        OrderStatus st = parseStatus(td.status);
        Instant created = td.createdAtEpochMs != null ? Instant.ofEpochMilli(td.createdAtEpochMs) : Instant.now();
        boolean hist = Boolean.TRUE.equals(td.inClosedHistory);
        if (st == OrderStatus.PAID) {
            hist = true;
        }
        if (td.payments != null && td.payments.stream().anyMatch(p -> p != null && p.amount != null
                && new BigDecimal(p.amount).signum() < 0)) {
            hist = true;
        }
        BigDecimal recordedTip = null;
        if (td.recordedTipTotal != null && !td.recordedTipTotal.isBlank()) {
            try {
                recordedTip = new BigDecimal(td.recordedTipTotal);
            } catch (NumberFormatException ignored) {
                recordedTip = null;
            }
        }
        Ticket ticket = Ticket.restoreFromPersistence(
                td.id, td.waiterId, td.tableId,
                td.orderNumber != null ? td.orderNumber : 0,
                td.guestCount != null ? td.guestCount : 1,
                st, created, hist, recordedTip);
        if (td.items != null) {
            for (ItemDto id : td.items) {
                if (id == null || id.menuItemId == null) {
                    continue;
                }
                Optional<MenuItem> mi = menu.itemById(id.menuItemId);
                if (mi.isEmpty()) {
                    continue;
                }
                TicketItem line = new TicketItem(mi.get());
                line.setSeat(id.seat != null ? id.seat : 1);
                line.setQuantity(id.quantity != null ? id.quantity : 1);
                line.setNotes(id.notes != null ? id.notes : "");
                line.setPaidQuantity(id.paidQuantity != null ? id.paidQuantity : 0);
                line.setRefundedQuantity(id.refundedQuantity != null ? id.refundedQuantity : 0);
                line.setSentToKitchen(Boolean.TRUE.equals(id.sentToKitchen));
                if (id.unitExtra != null) {
                    line.setUnitExtra(new BigDecimal(id.unitExtra));
                }
                ticket.items().add(line);
            }
        }
        if (td.payments != null) {
            for (PayDto pd : td.payments) {
                if (pd == null || pd.amount == null || pd.method == null) {
                    continue;
                }
                Instant at = pd.atEpochMs != null ? Instant.ofEpochMilli(pd.atEpochMs) : Instant.now();
                ticket.payments().add(new Payment(new BigDecimal(pd.amount), parseMethod(pd.method), at));
            }
        }
        return Optional.of(ticket);
    }

    private static TicketDto toDto(Ticket t) {
        TicketDto td = new TicketDto();
        td.id = t.id();
        td.waiterId = t.waiterId();
        td.tableId = t.tableId();
        td.orderNumber = t.orderNumber();
        td.guestCount = t.guestCount();
        td.status = t.status().name();
        td.createdAtEpochMs = t.createdAt().toEpochMilli();
        td.inClosedHistory = t.inClosedHistory();
        td.recordedTipTotal = t.recordedTipTotal().toPlainString();
        td.items = new ArrayList<>();
        for (TicketItem it : t.items()) {
            ItemDto id = new ItemDto();
            id.menuItemId = it.menuItem().id();
            id.seat = it.seat();
            id.quantity = it.quantity();
            id.notes = it.notes();
            id.paidQuantity = it.paidQuantity();
            id.refundedQuantity = it.refundedQuantity();
            id.sentToKitchen = it.isSentToKitchen();
            id.unitExtra = it.unitExtra().toPlainString();
            td.items.add(id);
        }
        td.payments = new ArrayList<>();
        for (Payment p : t.payments()) {
            PayDto pd = new PayDto();
            pd.amount = p.amount().toPlainString();
            pd.method = p.method().name();
            pd.atEpochMs = p.at().toEpochMilli();
            td.payments.add(pd);
        }
        return td;
    }

    private static OrderStatus parseStatus(String s) {
        if (s == null) {
            return OrderStatus.DRAFT;
        }
        try {
            return OrderStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return OrderStatus.DRAFT;
        }
    }

    private static PaymentMethod parseMethod(String s) {
        try {
            return PaymentMethod.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PaymentMethod.CASH;
        }
    }

    @SuppressWarnings("unused")
    private static final class RootDto {
        public Integer lastAssignedOrderNumber;
        public List<TicketDto> tickets;
    }

    @SuppressWarnings("unused")
    private static final class TicketDto {
        public String id;
        public String waiterId;
        public String tableId;
        public Integer orderNumber;
        public Integer guestCount;
        public String status;
        public Long createdAtEpochMs;
        public Boolean inClosedHistory;
        public String recordedTipTotal;
        public List<ItemDto> items;
        public List<PayDto> payments;
    }

    @SuppressWarnings("unused")
    private static final class ItemDto {
        public String menuItemId;
        public Integer seat;
        public Integer quantity;
        public String notes;
        public Integer paidQuantity;
        public Integer refundedQuantity;
        public Boolean sentToKitchen;
        public String unitExtra;
    }

    @SuppressWarnings("unused")
    private static final class PayDto {
        public String amount;
        public String method;
        public Long atEpochMs;
    }
}
