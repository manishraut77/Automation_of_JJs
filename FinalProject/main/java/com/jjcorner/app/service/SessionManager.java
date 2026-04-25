package com.jjcorner.app.service;

import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.Role;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stores the currently logged-in employee, demo-mode flag, and clock-in timestamps.
 */
public final class SessionManager {
    private final ObjectProperty<Employee> currentUser = new SimpleObjectProperty<>();
    private final Map<String, Instant> clockInAtByEmployeeId = new HashMap<>();
    private boolean demoMode;

    public ObjectProperty<Employee> currentUserProperty() {
        return currentUser;
    }

    public Employee currentUser() {
        return currentUser.get();
    }

    public boolean isLoggedIn() {
        return currentUser() != null;
    }

    public Role roleOrNull() {
        Employee u = currentUser();
        return u == null ? null : u.role();
    }

    public Role requireRole() {
        Role role = roleOrNull();
        return Objects.requireNonNull(role, "No role: user not logged in");
    }

    public void setCurrentUser(Employee employee) {
        currentUser.set(employee);
    }

    /**
     * Return to login screen without ending any clock-ins.
     * The next user must authenticate again to take control.
     */
    public void lock() {
        currentUser.set(null);
        demoMode = false;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public boolean isClockedIn() {
        Employee u = currentUser();
        if (u == null) return false;
        return clockInAtByEmployeeId.containsKey(u.employeeId());
    }

    public void clockIn() {
        Employee u = currentUser();
        if (u == null) {
            throw new IllegalStateException("Not logged in");
        }
        clockInAtByEmployeeId.put(u.employeeId(), Instant.now());
    }

    public void clockOut() {
        Employee u = currentUser();
        if (u == null) return;
        clockInAtByEmployeeId.remove(u.employeeId());
    }

    public Duration elapsedClockedIn() {
        Employee u = currentUser();
        if (u == null) return Duration.ZERO;
        Instant at = clockInAtByEmployeeId.get(u.employeeId());
        if (at == null) return Duration.ZERO;
        return Duration.between(at, Instant.now());
    }

    public void clear() {
        currentUser.set(null);
        clockInAtByEmployeeId.clear();
        demoMode = false;
    }
}

