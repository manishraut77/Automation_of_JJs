package com.jjcorner.app.model;

/**
 * Employee profile for staff who manage tables, orders, and checkout.
 */
public final class Waiter extends Employee {
    public Waiter(String employeeId, String username, String password, String displayName) {
        super(employeeId, username, password, Role.WAITER, displayName);
    }
}

