package com.jjcorner.app.model;

/**
 * Employee profile for staff who clean dirty tables.
 */
public final class Busboy extends Employee {
    public Busboy(String employeeId, String username, String password, String displayName) {
        super(employeeId, username, password, Role.BUSBOY, displayName);
    }
}

