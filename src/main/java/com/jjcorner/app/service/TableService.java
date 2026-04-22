package com.jjcorner.app.service;

import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.RestaurantTable;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.model.TableStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TableService {
    private final SessionManager session;
    private final ObservableList<RestaurantTable> realTables = FXCollections.observableArrayList();
    private final Map<String, ObservableList<RestaurantTable>> demoTablesByEmployeeId = new HashMap<>();

    public TableService(SessionManager session) {
        this.session = session;
        initTables(realTables);
    }

    public ObservableList<RestaurantTable> allTables() {
        return activeTables();
    }

    public RestaurantTable requireTable(String id) {
        return activeTables().stream().filter(t -> t.id().equalsIgnoreCase(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown table: " + id));
    }

    public List<RestaurantTable> waiterAssignedTables(String waiterId) {
        if (waiterId == null) return List.of();
        return activeTables().stream().filter(t -> t.isAssignedTo(waiterId)).sorted(Comparator.comparing(RestaurantTable::id)).toList();
    }

    /** True if this waiter still has any assigned table in use (yellow). Clock-out should be blocked until Open or Dirty. */
    public boolean waiterHasOccupiedAssignedTable(String waiterId) {
        if (waiterId == null) return false;
        return activeTables().stream()
                .anyMatch(t -> t.isAssignedTo(waiterId) && t.status() == TableStatus.OCCUPIED);
    }

    public void ensureAssignmentsForCurrentUser() {
        Employee u = session.currentUser();
        if (u == null || u.role() != Role.WAITER) return;
        String waiterId = u.employeeId();
        ObservableList<RestaurantTable> list = activeTables();
        boolean alreadyAssigned = list.stream().anyMatch(t -> waiterId.equals(t.assignedWaiterId()));
        if (alreadyAssigned) return;
        assignWaiterTables(list, waiterId);
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

        if (!session.isClockedIn()) {
            throw new IllegalStateException("Error: You are not clocked in");
        }

        TableStatus current = table.status();
        if (actorRole == Role.BUSBOY) {
            if (current == TableStatus.DIRTY && nextStatus == TableStatus.OPEN) {
                table.setGuestCount(0);
                table.setStatus(TableStatus.OPEN);
                return;
            }
            if (nextStatus == TableStatus.OCCUPIED) {
                throw new IllegalArgumentException("Error: busboys cannot mark tables as occupied");
            }
            throw new IllegalArgumentException("Busboy can only change Dirty -> Open");
        }

        if (actorRole == Role.WAITER) {
            if (current == TableStatus.OPEN && nextStatus == TableStatus.OCCUPIED) {
                table.setStatus(TableStatus.OCCUPIED);
                return;
            }
            if (current == TableStatus.OCCUPIED && nextStatus == TableStatus.DIRTY) {
                table.setStatus(TableStatus.DIRTY);
                return;
            }
            if (current == TableStatus.DIRTY && nextStatus == TableStatus.OPEN) {
                throw new IllegalArgumentException("Error: Waiters cannot mark tables as Open after cleaning");
            }
            throw new IllegalArgumentException("Waiter can only change Open -> Occupied or Occupied -> Dirty");
        }

        throw new IllegalArgumentException("Unsupported role for table status changes: " + actorRole);
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
        // Layout matches the provided floor plan (28 tables total):
        // - Left block: A,B rows 1-6 (12)
        // - Right block: E,F rows 1-6 (12)
        // - Bottom middle: C,D rows 5-6 (4)
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

    private void assignWaiterTables(List<RestaurantTable> base, String waiterId) {
        List<RestaurantTable> list = new ArrayList<>(base);
        list.sort(Comparator.comparing(RestaurantTable::id));

        // Deterministic spread based on waiterId hash
        int seed = Math.abs(waiterId.hashCode());
        int count = 12; // keeps "View My Tables" meaningful
        for (int i = 0; i < count; i++) {
            int idx = (seed + i * 5) % list.size();
            list.get(idx).setAssignedWaiterId(waiterId);
        }
    }
}

