package com.jjcorner.app.model;

public final class Manager extends Employee {
    public Manager(String employeeId, String username, String password, String displayName) {
        super(employeeId, username, password, Role.MANAGER, displayName);
    }
}