package com.jjcorner.app.model;

public final class Waiter extends Employee {
    public Waiter(String employeeId, String username, String password, String displayName) {
        super(employeeId, username, password, Role.WAITER, displayName);
    }
}

