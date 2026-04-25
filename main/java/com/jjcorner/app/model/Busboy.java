package com.jjcorner.app.model;

public final class Busboy extends Employee {
    public Busboy(String employeeId, String username, String password, String displayName) {
        super(employeeId, username, password, Role.BUSBOY, displayName);
    }
}

