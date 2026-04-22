package com.jjcorner.app.service;

import com.jjcorner.app.model.Busboy;
import com.jjcorner.app.model.Employee;
import com.jjcorner.app.model.Role;
import com.jjcorner.app.model.Waiter;
import com.jjcorner.app.persist.EmployeeLedger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AuthService {
    private static final Pattern EMP_ID = Pattern.compile("^[A-Za-z0-9]{6}$");
    private final Map<String, Employee> usersByUsername = new HashMap<>();

    public AuthService() {
        List<Employee> loaded = EmployeeLedger.load();
        if (loaded.isEmpty()) {
            // Seed demo users (persisted so multiple profiles work across restarts)
            createInternal(new Waiter("WTR001", "waiter1", "a1b2c3", "Waiter One"));
            createInternal(new Busboy("BUS001", "busboy1", "b1c2d3", "Busboy One"));
            createInternal(new Employee("MGR001", "manager1", "m1n2g3", Role.MANAGER, "Manager One"));
            EmployeeLedger.save(usersByUsername.values().stream().toList());
        } else {
            loaded.forEach(this::createInternal);
        }
    }

    public Optional<Employee> login(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        Employee u = usersByUsername.get(username.trim().toLowerCase());
        if (u == null) return Optional.empty();
        return Objects.equals(u.password(), password) ? Optional.of(u) : Optional.empty();
    }

    public Employee signUp(String employeeId, String username, String password, Role role, String displayName) {
        employeeId = employeeId == null ? "" : employeeId.trim();
        username = username == null ? "" : username.trim();
        password = password == null ? "" : password.trim();

        if (!EMP_ID.matcher(employeeId).matches()) {
            throw new IllegalArgumentException("Employee ID must be exactly 6 alphanumeric characters.");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (usersByUsername.containsKey(username.toLowerCase())) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        validatePassword(password);

        Employee created = switch (Objects.requireNonNull(role)) {
            case WAITER -> new Waiter(employeeId, username, password, displayName);
            case BUSBOY -> new Busboy(employeeId, username, password, displayName);
            default -> throw new IllegalArgumentException("Only Waiter and Busboy sign-up are supported here.");
        };
        createInternal(created);
        EmployeeLedger.save(usersByUsername.values().stream().toList());
        return created;
    }

    private void createInternal(Employee employee) {
        usersByUsername.put(employee.username().trim().toLowerCase(), employee);
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (isUniform(password)) {
            throw new IllegalArgumentException("Password cannot be uniform (e.g., AAAAAA).");
        }
        if (hasConsecutiveNumberRun(password, 4)) {
            throw new IllegalArgumentException("Password cannot contain consecutive numbers (e.g., 1234567).");
        }
    }

    private static boolean isUniform(String s) {
        char first = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != first) return false;
        }
        return true;
    }

    private static boolean hasConsecutiveNumberRun(String s, int minLen) {
        int run = 1;
        for (int i = 0; i < s.length() - 1; i++) {
            char a = s.charAt(i);
            char b = s.charAt(i + 1);
            if (Character.isDigit(a) && Character.isDigit(b) && (b == a + 1)) {
                run++;
                if (run >= minLen) return true;
            } else {
                run = 1;
            }
        }
        return false;
    }
}

