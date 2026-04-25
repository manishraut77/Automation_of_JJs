package com.jjcorner.app.service;

import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.RestaurantTable;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.model.TableStatus;
import com.jjcorner.app.persist.TableLedger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Owns table layout, table status transitions, waiter assignments, and joined-table behavior.
 */
public final class TableService {
    private final SessionManager session;
    private final ActivityService activity;
    private final ObservableList<RestaurantTable> realTables = FXCollections.observableArrayList();
    private final Map<String, ObservableList<RestaurantTable>> demoTablesByEmployeeId = new HashMap<>();

    public TableService(SessionManager session, ActivityService activity) {
        this.session = session;
        this.activity = activity;
        List<RestaurantTable> loaded = TableLedger.load();
        if (loaded.isEmpty()) {
            initTables(realTables);
            persist();
        } else {
            realTables.addAll(loaded);
            ensureDefaultTables(realTables);
        }
    }

    public ObservableList<RestaurantTable> allTables() {
        return activeTables();
    }

    public RestaurantTable requireTable(String id) {
        return activeTables().stream().filter(t -> t.id().equalsIgnoreCase(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown table: " + id));
    }

    public RestaurantTable primaryTableFor(String id) {
        List<RestaurantTable> group = joinedGroupFor(id);
        return group.stream().min(Comparator.comparing(RestaurantTable::id)).orElseGet(() -> requireTable(id));
    }

    public RestaurantTable primaryTableFor(RestaurantTable table) {
        Objects.requireNonNull(table);
        return primaryTableFor(table.id());
    }

    public List<RestaurantTable> joinedGroupFor(String id) {
        RestaurantTable start = requireTable(id);
        Set<String> visited = new LinkedHashSet<>();
        collectJoined(start, visited);
        return visited.stream().map(this::requireTable).sorted(Comparator.comparing(RestaurantTable::id)).toList();
    }

    public List<RestaurantTable> joinedGroupFor(RestaurantTable table) {
        Objects.requireNonNull(table);
        return joinedGroupFor(table.id());
    }

    public boolean isPrimaryJoinedTable(RestaurantTable table) {
        return table != null && primaryTableFor(table).id().equalsIgnoreCase(table.id());
    }

    public String displayIdForGroup(RestaurantTable table) {
        List<RestaurantTable> group = joinedGroupFor(table);
        if (group.size() <= 1) {
            return table.id();
        }
        return group.stream().map(RestaurantTable::id).toList().toString().replace("[", "").replace("]", "");
    }

    public int totalSeatsForGroup(RestaurantTable table) {
        return joinedGroupFor(table).stream().mapToInt(RestaurantTable::seatCount).sum();
    }

    public boolean groupAssignedTo(RestaurantTable table, String waiterId) {
        if (waiterId == null) {
            return false;
        }
        return joinedGroupFor(table).stream().anyMatch(t -> t.isAssignedTo(waiterId));
    }

    public List<RestaurantTable> waiterAssignedTables(String waiterId) {
        if (waiterId == null) {
            return List.of();
        }
        return activeTables().stream()
                .filter(t -> t.isAssignedTo(waiterId))
                .sorted(Comparator.comparing(RestaurantTable::id))
                .toList();
    }

    /**
     * Returns true when a waiter still has an occupied assigned table. Clock-out is blocked until
     * those tables are moved to dirty or open.
     */
    public boolean waiterHasOccupiedAssignedTable(String waiterId) {
        if (waiterId == null) {
            return false;
        }
        return activeTables().stream()
                .anyMatch(t -> t.isAssignedTo(waiterId) && t.status() == TableStatus.OCCUPIED);
    }

    public void ensureAssignmentsForCurrentUser() {
        Employee u = session.currentUser();
        if (u == null || u.role() != Role.WAITER) {
            return;
        }
        String waiterId = u.employeeId();
        ObservableList<RestaurantTable> list = activeTables();
        List<RestaurantTable> expected = assignedTablesForWaiter(list, waiterId);
        boolean allExpectedAssigned = expected.stream().allMatch(t -> waiterId.equals(t.assignedWaiterId()));
        if (allExpectedAssigned) {
            return;
        }
        assignWaiterTables(expected, waiterId);
    }

    public void resetDemoForCurrentUser() {
        if (!session.isDemoMode()) {
            return;
        }
        Employee u = session.currentUser();
        String key = u == null ? "anon" : u.employeeId();
        demoTablesByEmployeeId.remove(key);
    }

    public void attemptStatusChange(Role actorRole, RestaurantTable table, TableStatus nextStatus) {
        Objects.requireNonNull(actorRole);
        Objects.requireNonNull(table);
        Objects.requireNonNull(nextStatus);

        if (!session.isClockedIn() && actorRole != Role.MANAGER) {
            throw new IllegalStateException("Error: You are not clocked in");
        }

        RestaurantTable primary = primaryTableFor(table);
        List<RestaurantTable> group = joinedGroupFor(primary);
        TableStatus current = primary.status();
        if (actorRole == Role.BUSBOY) {
            if (current == TableStatus.DIRTY && nextStatus == TableStatus.OPEN) {
                applyGroupState(group, TableStatus.OPEN, 0);
                activity.record(session.currentUser(), "Marked table " + displayIdForGroup(primary) + " open");
                persist();
                return;
            }
            if (nextStatus == TableStatus.OCCUPIED) {
                throw new IllegalArgumentException("Error: busboys cannot mark tables as occupied");
            }
            throw new IllegalArgumentException("Busboy can only change Dirty -> Open");
        }

        if (actorRole == Role.WAITER) {
            if (current == TableStatus.OPEN && nextStatus == TableStatus.OCCUPIED) {
                applyGroupState(group, TableStatus.OCCUPIED, Math.max(1, primary.guestCount()));
                activity.record(session.currentUser(), "Marked table " + displayIdForGroup(primary) + " occupied");
                persist();
                return;
            }
            if (current == TableStatus.OCCUPIED && nextStatus == TableStatus.DIRTY) {
                applyGroupState(group, TableStatus.DIRTY, primary.guestCount());
                activity.record(session.currentUser(), "Marked table " + displayIdForGroup(primary) + " dirty");
                persist();
                return;
            }
            if (current == TableStatus.DIRTY && nextStatus == TableStatus.OPEN) {
                throw new IllegalArgumentException("Error: Waiters cannot mark tables as Open after cleaning");
            }
            throw new IllegalArgumentException("Waiter can only change Open -> Occupied or Occupied -> Dirty");
        }

        if (actorRole == Role.MANAGER) {
            int guests = nextStatus == TableStatus.OPEN ? 0 : Math.max(1, primary.guestCount());
            applyGroupState(group, nextStatus, guests);
            activity.record(session.currentUser(),
                    "Marked table " + displayIdForGroup(primary) + " " + nextStatus.name().toLowerCase());
            persist();
            return;
        }

        throw new IllegalArgumentException("Unsupported role for table status changes: " + actorRole);
    }

    public void updateTableLayout(RestaurantTable table, String assignedWaiterId, int seatCount, String joinedTableIds) {
        Objects.requireNonNull(table);
        table.setAssignedWaiterId(assignedWaiterId == null || assignedWaiterId.isBlank() ? null : assignedWaiterId.trim());
        table.setSeatCount(seatCount);
        table.setJoinedTableIds(normalizeJoinedIds(table, joinedTableIds));
        persist();
    }

    public void addTable(String id, int seatCount, String assignedWaiterId) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Table ID is required.");
        }
        String normalized = id.trim().toUpperCase();
        if (activeTables().stream().anyMatch(t -> t.id().equalsIgnoreCase(normalized))) {
            throw new IllegalArgumentException("Table ID already exists.");
        }
        RestaurantTable table = new RestaurantTable(normalized);
        table.setSeatCount(seatCount);
        table.setAssignedWaiterId(assignedWaiterId);
        activeTables().add(table);
        persist();
    }

    private ObservableList<RestaurantTable> activeTables() {
        if (!session.isDemoMode()) {
            return realTables;
        }
        Employee u = session.currentUser();
        String key = u == null ? "anon" : u.employeeId();
        return demoTablesByEmployeeId.computeIfAbsent(key, k -> {
            ObservableList<RestaurantTable> demo = FXCollections.observableArrayList();
            initTables(demo);
            return demo;
        });
    }

    private void initTables(ObservableList<RestaurantTable> tables) {
        tables.clear();
        // Default floor plan: side blocks A/B and E/F, plus bottom-center C/D tables.
        for (char col : new char[]{'A', 'B', 'E', 'F'}) {
            for (int row = 1; row <= 6; row++) {
                tables.add(new RestaurantTable("" + col + row));
            }
        }
        for (char col : new char[]{'C', 'D'}) {
            for (int row = 5; row <= 6; row++) {
                tables.add(new RestaurantTable("" + col + row));
            }
        }
    }

    private void collectJoined(RestaurantTable table, Set<String> visited) {
        if (table == null || !visited.add(table.id())) {
            return;
        }
        for (String joinedId : parseJoinedIds(table.joinedTableIds())) {
            if (activeTables().stream().anyMatch(t -> t.id().equalsIgnoreCase(joinedId))) {
                collectJoined(requireTable(joinedId), visited);
            }
        }
        for (RestaurantTable other : activeTables()) {
            if (other.id().equalsIgnoreCase(table.id())) {
                continue;
            }
            if (parseJoinedIds(other.joinedTableIds()).stream().anyMatch(x -> x.equalsIgnoreCase(table.id()))) {
                collectJoined(other, visited);
            }
        }
    }

    private List<String> parseJoinedIds(String joinedTableIds) {
        if (joinedTableIds == null || joinedTableIds.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String raw : joinedTableIds.split("[,\\s]+")) {
            String id = raw.trim().toUpperCase();
            if (!id.isBlank()) {
                out.add(id);
            }
        }
        return out;
    }

    private String normalizeJoinedIds(RestaurantTable table, String joinedTableIds) {
        Set<String> ids = new LinkedHashSet<>();
        for (String id : parseJoinedIds(joinedTableIds)) {
            if (id.equalsIgnoreCase(table.id())) {
                continue;
            }
            requireTable(id);
            ids.add(id.toUpperCase());
        }
        return String.join(",", ids);
    }

    private void applyGroupState(List<RestaurantTable> group, TableStatus status, int guestCount) {
        int guests = Math.max(0, guestCount);
        for (RestaurantTable t : group) {
            t.setStatus(status);
            t.setGuestCount(status == TableStatus.OPEN ? 0 : guests);
        }
    }

    private void ensureDefaultTables(ObservableList<RestaurantTable> tables) {
        List<String> expected = new ArrayList<>();
        for (char col : new char[]{'A', 'B', 'E', 'F'}) {
            for (int row = 1; row <= 6; row++) {
                expected.add("" + col + row);
            }
        }
        for (char col : new char[]{'C', 'D'}) {
            for (int row = 5; row <= 6; row++) {
                expected.add("" + col + row);
            }
        }
        boolean changed = false;
        for (String id : expected) {
            boolean exists = tables.stream().anyMatch(t -> t.id().equalsIgnoreCase(id));
            if (!exists) {
                tables.add(new RestaurantTable(id));
                changed = true;
            }
        }
        if (changed) {
            persist();
        }
    }

    private void persist() {
        if (!session.isDemoMode()) {
            TableLedger.save(new ArrayList<>(realTables));
        }
    }

    private List<RestaurantTable> assignedTablesForWaiter(List<RestaurantTable> base, String waiterId) {
        List<RestaurantTable> list = new ArrayList<>(base);
        list.sort(Comparator.comparing(RestaurantTable::id));

        // Deterministic spread keeps assigned tables stable across restarts.
        int seed = Math.abs(waiterId.hashCode());
        int count = 12;
        List<RestaurantTable> assigned = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int idx = (seed + i * 5) % list.size();
            RestaurantTable table = list.get(idx);
            if (!assigned.contains(table)) {
                assigned.add(table);
            }
        }
        return assigned;
    }

    private void assignWaiterTables(List<RestaurantTable> assignedTables, String waiterId) {
        for (RestaurantTable table : assignedTables) {
            table.setAssignedWaiterId(waiterId);
        }
        persist();
    }
}
