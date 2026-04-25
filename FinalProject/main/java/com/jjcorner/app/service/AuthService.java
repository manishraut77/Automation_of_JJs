package com.jjcorner.app.service;

import com.jjcorner.app.model.*;
import com.jjcorner.app.persist.EmployeeLedger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Handles employee authentication, profile creation, and password validation.
 */
public final class AuthService {
    private static final Pattern EMP_ID = Pattern.compile("^[A-Za-z0-9]{6}$");
    private static final Pattern HAS_LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final List<String> BLOCKED_PASSWORDS = List.of(
            "password", "password1", "pass123", "admin", "admin123",
            "manager", "manager1", "waiter", "waiter1", "busboy", "cook",
            "123456", "1234567", "12345678", "111111", "000000",
            "qwerty", "qwerty1", "abc123", "abcdef", "letmein", "welcome"
    );
    private final Map<String, Employee> usersByUsername = new HashMap<>();

    public AuthService() {
        List<Employee> loaded = EmployeeLedger.load();
        if (loaded.isEmpty()) {
            seedRequiredProfiles();
        } else {
            loaded.forEach(this::createInternal);
            seedMissingRequiredProfiles();
        }
    }

    public Optional<Employee> login(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        Employee u = usersByUsername.get(username.trim().toLowerCase());
        if (u == null) return Optional.empty();
        return Objects.equals(u.password(), password) ? Optional.of(u) : Optional.empty();
    }

    public List<Employee> allEmployees() {
        return usersByUsername.values().stream()
                .sorted((a, b) -> a.employeeId().compareToIgnoreCase(b.employeeId()))
                .toList();
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
        if (employeeIdExists(employeeId, null)) {
            throw new IllegalArgumentException("Employee ID is already taken.");
        }
        validatePassword(password);

        Employee created = switch (Objects.requireNonNull(role)) {
            case WAITER -> new Waiter(employeeId, username, password, displayName);
            case BUSBOY -> new Busboy(employeeId, username, password, displayName);
            case MANAGER -> new Manager(employeeId, username, password, displayName);
            case COOK -> new Employee(employeeId, username, password, Role.COOK, displayName);
        };
        createInternal(created);
        EmployeeLedger.save(usersByUsername.values().stream().toList());
        return created;
    }

    public Employee updateEmployee(Employee original, String employeeId, String username, String password, Role role, String displayName) {
        if (original == null) {
            throw new IllegalArgumentException("Select an employee.");
        }
        employeeId = employeeId == null ? "" : employeeId.trim();
        username = username == null ? "" : username.trim();
        password = password == null ? "" : password.trim();
        if (!EMP_ID.matcher(employeeId).matches()) {
            throw new IllegalArgumentException("Employee ID must be exactly 6 alphanumeric characters.");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        String originalKey = original.username().trim().toLowerCase();
        String nextKey = username.toLowerCase();
        if (!originalKey.equals(nextKey) && usersByUsername.containsKey(nextKey)) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (employeeIdExists(employeeId, original.employeeId())) {
            throw new IllegalArgumentException("Employee ID is already taken.");
        }
        validatePassword(password);

        Employee updated = switch (Objects.requireNonNull(role)) {
            case WAITER -> new Waiter(employeeId, username, password, displayName);
            case BUSBOY -> new Busboy(employeeId, username, password, displayName);
            case MANAGER -> new Manager(employeeId, username, password, displayName);
            case COOK -> new Employee(employeeId, username, password, Role.COOK, displayName);
        };
        usersByUsername.remove(originalKey);
        createInternal(updated);
        EmployeeLedger.save(usersByUsername.values().stream().toList());
        return updated;
    }

    public void deleteEmployee(Employee employee) {
        if (employee == null) {
            return;
        }
        usersByUsername.remove(employee.username().trim().toLowerCase());
        EmployeeLedger.save(usersByUsername.values().stream().toList());
    }

    private void createInternal(Employee employee) {
        usersByUsername.put(employee.username().trim().toLowerCase(), employee);
    }

    private boolean employeeIdExists(String employeeId, String allowedCurrentId) {
        return usersByUsername.values().stream()
                .anyMatch(e -> e.employeeId().equalsIgnoreCase(employeeId)
                        && (allowedCurrentId == null || !e.employeeId().equalsIgnoreCase(allowedCurrentId)));
    }

    private void seedRequiredProfiles() {
        createInternal(new Waiter("WTR001", "waiter1", "a1b2c3", "Waiter One"));
        createInternal(new Waiter("WTR002", "waiter2", "a2b3c4", "Waiter Two"));
        createInternal(new Waiter("WTR003", "waiter3", "a3b4c5", "Waiter Three"));
        createInternal(new Busboy("BUS001", "busboy1", "b1c2d3", "Busboy One"));
        createInternal(new Employee("CK001A", "cook1", "c1d2e3", Role.COOK, "Cook One"));
        createInternal(new Employee("CK002A", "cook2", "c2d3e4", Role.COOK, "Cook Two"));
        createInternal(new Employee("CK003A", "cook3", "c3d4e5", Role.COOK, "Cook Three"));
        createInternal(new Employee("MGR001", "manager1", "m1n2g3", Role.MANAGER, "Manager One"));
        EmployeeLedger.save(usersByUsername.values().stream().toList());
    }

    private void seedMissingRequiredProfiles() {
        boolean changed = false;
        changed |= addIfMissing(new Waiter("WTR002", "waiter2", "a2b3c4", "Waiter Two"));
        changed |= addIfMissing(new Waiter("WTR003", "waiter3", "a3b4c5", "Waiter Three"));
        changed |= addIfMissing(new Employee("CK001A", "cook1", "c1d2e3", Role.COOK, "Cook One"));
        changed |= addIfMissing(new Employee("CK002A", "cook2", "c2d3e4", Role.COOK, "Cook Two"));
        changed |= addIfMissing(new Employee("CK003A", "cook3", "c3d4e5", Role.COOK, "Cook Three"));
        if (changed) {
            EmployeeLedger.save(usersByUsername.values().stream().toList());
        }
    }

    private boolean addIfMissing(Employee employee) {
        if (usersByUsername.containsKey(employee.username().toLowerCase()) || employeeIdExists(employee.employeeId(), null)) {
            return false;
        }
        createInternal(employee);
        return true;
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        String normalized = password.trim().toLowerCase();
        if (BLOCKED_PASSWORDS.contains(normalized)) {
            throw new IllegalArgumentException("Password is too common or easy to guess.");
        }
        if (!HAS_LETTER.matcher(password).matches() || !HAS_DIGIT.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must include at least one letter and one number.");
        }
        if (password.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Password cannot be numbers only.");
        }
        if (isUniform(password)) {
            throw new IllegalArgumentException("Password cannot be uniform (e.g., AAAAAA).");
        }
        if (isRepeatedPattern(password)) {
            throw new IllegalArgumentException("Password cannot be a repeated pattern (e.g., abcabc).");
        }
        if (hasConsecutiveNumberRun(password, 4)) {
            throw new IllegalArgumentException("Password cannot contain consecutive numbers (e.g., 1234567).");
        }
        if (hasKeyboardSequence(normalized)) {
            throw new IllegalArgumentException("Password cannot contain keyboard sequences (e.g., qwerty).");
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

    private static boolean isRepeatedPattern(String s) {
        String lower = s.toLowerCase();
        for (int len = 1; len <= lower.length() / 2; len++) {
            if (lower.length() % len != 0) continue;
            String part = lower.substring(0, len);
            if (part.repeat(lower.length() / len).equals(lower)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasKeyboardSequence(String s) {
        String[] sequences = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
        for (String seq : sequences) {
            for (int i = 0; i <= seq.length() - 4; i++) {
                String chunk = seq.substring(i, i + 4);
                if (s.contains(chunk)) return true;
            }
        }
        return false;
    }
}
