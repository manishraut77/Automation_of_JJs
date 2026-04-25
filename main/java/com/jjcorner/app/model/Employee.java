package com.jjcorner.app.model;

import java.util.Objects;

public class Employee {
    private final String employeeId;
    private final String username;
    private final String password;
    private final Role role;
    private final String displayName;

    public Employee(String employeeId, String username, String password, Role role, String displayName) {
        this.employeeId = Objects.requireNonNull(employeeId);
        this.username = Objects.requireNonNull(username);
        this.password = Objects.requireNonNull(password);
        this.role = Objects.requireNonNull(role);
        this.displayName = displayName == null ? username : displayName;
    }

    public String employeeId() {
        return employeeId;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public Role role() {
        return role;
    }

    public String displayName() {
        return displayName;
    }
}

